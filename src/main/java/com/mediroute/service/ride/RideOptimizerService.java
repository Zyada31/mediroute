//package com.mediroute.service.ride;
//
//import com.google.ortools.constraintsolver.*;
//import com.google.protobuf.Duration;
//import com.mediroute.dto.RideAssignmentAuditDTO;
//import com.mediroute.entity.AssignmentAudit;
//import com.mediroute.entity.Driver;
//import com.mediroute.entity.Ride;
//import com.mediroute.repository.AssignmentAuditRepository;
//import com.mediroute.repository.DriverRepository;
//import com.mediroute.repository.PatientRepository;
//import com.mediroute.repository.RideRepository;
//import com.mediroute.service.distance.OsrmDistanceService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RideOptimizerService {
//
//    private final RideRepository rideRepository;
//    private final DriverRepository driverRepository;
//    private final OsrmDistanceService distanceService;
//    private final AssignmentAuditRepository assignmentAuditRepository;
//
//    static {
//        System.loadLibrary("jniortools");
//    }
//
//    public void optimizeSchedule(List<Ride> rides) {
//        if (rides.isEmpty()) {
//            log.warn("⚠️ No rides to optimize.");
//            return;
//        }
//
//        List<Driver> drivers = driverRepository.findByActiveTrue();
//        if (drivers.isEmpty()) {
//            log.warn("⚠️ No active drivers available.");
//            return;
//        }
//
//        log.info("🧠 Assigning {} rides to {} drivers (pickup+dropoff+return, miles optimized)...",
//                rides.size(), drivers.size());
//
//        // Build locations
//        List<String> locations = new ArrayList<>();
//        Map<Integer, String> indexToEntity = new HashMap<>();
//        int index = 0;
//
//        // Add driver bases (jitter if identical)
//        for (int i = 0; i < drivers.size(); i++) {
//            Driver d = drivers.get(i);
//            double jitterLat = d.getBaseLat() + (Math.random() - 0.5) / 10000;
//            double jitterLng = d.getBaseLng() + (Math.random() - 0.5) / 10000;
//            locations.add(jitterLng + "," + jitterLat);
//            indexToEntity.put(index++, "DRIVER:" + d.getId());
//        }
//
//        // Add pickups and dropoffs
//        for (Ride r : rides) {
//            locations.add(r.getPickupLng() + "," + r.getPickupLat());
//            indexToEntity.put(index++, "PICKUP:" + r.getId());
//
//            locations.add(r.getDropoffLng() + "," + r.getDropoffLat());
//            indexToEntity.put(index++, "DROPOFF:" + r.getId());
//        }
//
//        double[][] costMatrix = distanceService.getDistanceMatrix(locations);
//        int numDrivers = drivers.size();
//        int numNodes = locations.size();
//
//        int[] starts = new int[numDrivers];
//        int[] ends = new int[numDrivers];
//        for (int i = 0; i < numDrivers; i++) {
//            starts[i] = i;
//            ends[i] = i;
//        }
//
//        RoutingIndexManager manager = new RoutingIndexManager(numNodes, numDrivers, starts, ends);
//        RoutingModel routing = new RoutingModel(manager);
//
//        // Distance callback
//        int transitCallbackIndex = routing.registerTransitCallback((fromIdx, toIdx) -> {
//            int fromNode = manager.indexToNode((int) fromIdx);
//            int toNode = manager.indexToNode((int) toIdx);
//            return (long) costMatrix[fromNode][toNode];
//        });
//        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);
//
//        // Capacity: +1 pickup, -1 dropoff
//        int[] demands = new int[numNodes];
//        for (Map.Entry<Integer, String> entry : indexToEntity.entrySet()) {
//            String entity = entry.getValue();
//            if (entity.startsWith("PICKUP:")) demands[entry.getKey()] = 1;
//            if (entity.startsWith("DROPOFF:")) demands[entry.getKey()] = -1;
//        }
//
//        long[] capacities = drivers.stream()
//                .mapToLong(d -> Optional.ofNullable(d.getMaxDailyRides()).orElse(10))
//                .toArray();
//
//        int demandCallbackIdx = routing.registerUnaryTransitCallback(fromIdx -> {
//            int fromNode = manager.indexToNode((int) fromIdx);
//            return demands[fromNode];
//        });
//        routing.addDimensionWithVehicleCapacity(demandCallbackIdx, 0, capacities, true, "Capacity");
//
//        // Precedence constraints (pickup before dropoff)
//        try {
//            for (Ride r : rides) {
//                Integer pickupIdx = null, dropoffIdx = null;
//                for (Map.Entry<Integer, String> entry : indexToEntity.entrySet()) {
//                    if (entry.getValue().equals("PICKUP:" + r.getId())) pickupIdx = Math.toIntExact(manager.nodeToIndex(entry.getKey()));
//                    if (entry.getValue().equals("DROPOFF:" + r.getId())) dropoffIdx = Math.toIntExact(manager.nodeToIndex(entry.getKey()));
//                }
//                if (pickupIdx != null && dropoffIdx != null) {
//                    routing.addPickupAndDelivery(pickupIdx, dropoffIdx);
//                    routing.solver().addConstraint(
//                            routing.solver().makeEquality(routing.vehicleVar(pickupIdx), routing.vehicleVar(dropoffIdx))
//                    );
//                }
//            }
//        } catch (Exception e) {
//            log.error("⚠️ Failed adding pickup/dropoff precedence, continuing without it.", e);
//        }
//
//        // Penalties (allow skipping pickups and dropoffs)
//        for (Map.Entry<Integer, String> entry : indexToEntity.entrySet()) {
//            if (entry.getValue().startsWith("PICKUP:") || entry.getValue().startsWith("DROPOFF:")) {
//                routing.addDisjunction(new long[]{manager.nodeToIndex(entry.getKey())}, 10000);
//            }
//        }
//
//        RoutingSearchParameters parameters = main.defaultRoutingSearchParameters()
//                .toBuilder()
//                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
//                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
//                .setTimeLimit(Duration.newBuilder().setSeconds(15).build())
//                .build();
//
//        Assignment solution;
//        try {
//            solution = routing.solveWithParameters(parameters);
//        } catch (Exception e) {
//            log.error("❌ OR-Tools failed. Retrying without precedence constraints.", e);
//            fallbackOptimize(rides, drivers, costMatrix, indexToEntity, manager);
//            return;
//        }
//
//        if (solution == null) {
//            log.warn("❌ No solution found. Falling back...");
//            fallbackOptimize(rides, drivers, costMatrix, indexToEntity, manager);
//            return;
//        }
//
//        // Apply assignments
//        Map<Long, List<Long>> summaryMap = new HashMap<>();
//        for (int driverIdx = 0; driverIdx < numDrivers; driverIdx++) {
//            Driver driver = drivers.get(driverIdx);
//            long indexRoute = routing.start(driverIdx);
//
//            List<Long> rideIds = new ArrayList<>();
//            while (!routing.isEnd(indexRoute)) {
//                int node = manager.indexToNode((int) indexRoute);
//                String entity = indexToEntity.get(node);
//
//                if (entity != null && entity.startsWith("PICKUP:")) {
//                    Long rideId = Long.parseLong(entity.split(":")[1]);
//                    Ride ride = rideRepository.findById(rideId).orElse(null);
//                    if (ride != null) {
//                        ride.setDriver(driver);
//                        rideRepository.save(ride);
//                        rideIds.add(ride.getId());
//                    }
//                }
//                indexRoute = solution.value(routing.nextVar(indexRoute));
//            }
//            if (!rideIds.isEmpty()) summaryMap.put(driver.getId(), rideIds);
//        }
//
//        // Audit
//        List<RideAssignmentAuditDTO> auditDTOs = rides.stream()
//                .map(r -> new RideAssignmentAuditDTO(
//                        r.getId(),
//                        r.getPatient().getName(),
//                        r.getPickupLocation(),
//                        r.getDropoffLocation(),
//                        r.getDriver() != null ? r.getDriver().getId() : null,
//                        r.getDriver() != null ? r.getDriver().getName() : "UNASSIGNED",
//                        "AUTO-SCHEDULER",
//                        "Pickup+Dropoff (with returns, fallback-safe)",
//                        LocalDateTime.now()
//                ))
//                .toList();
//
//        AssignmentAudit audit = new AssignmentAudit();
//        audit.setAssignmentTime(LocalDateTime.now());
//        audit.setAssignmentDate(rides.get(0).getPickupTime().toLocalDate());
//        audit.setTotalRides(rides.size());
//        audit.setAssignedDrivers(summaryMap.size());
//        audit.setUnassignedRides((int) rides.stream().filter(r -> r.getDriver() == null).count());
//        audit.setTriggeredBy("AUTO-SCHEDULER");
//        audit.setRideAssignmentsSummary(summaryMap);
//        audit.setRideAssignmentsDetail(auditDTOs);
//
//        assignmentAuditRepository.save(audit);
//
//        log.info("📅 Optimization complete. {} rides assigned (robust mode).", rides.size());
//    }
//
//    private void fallbackOptimize(List<Ride> rides,
//                                  List<Driver> drivers,
//                                  double[][] costMatrix,
//                                  Map<Integer, String> indexToEntity,
//                                  RoutingIndexManager manager) {
//        log.warn("⚠️ Running fallback optimization (no precedence).");
//        // TODO: simple greedy or distance-minimizing assignment here
//    }
//}