package com.mediroute.service;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.mediroute.entity.Ride;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.ortools.constraintsolver.RoutingSearchParameters.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideOptimizerService {

    private final RideRepository rideRepository;

    public void optimize(List<Ride> rides) {
        Loader.loadNativeLibraries();
        log.info("🚦 Starting optimization for {} rides", rides.size());

        rides = rides.stream()
                .filter(ride -> "scheduled".equalsIgnoreCase(ride.getStatus()))
                .collect(Collectors.toList());

        if (rides.isEmpty()) {
            log.info("ℹ️ No scheduled rides to optimize");
            return;
        }

        int numRides = rides.size();
        int numNodes = numRides + 1;
        int[][] distanceMatrix = new int[numNodes][numNodes];

        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                } else {
                    double fromDistance = (i == 0) ? 0 : rides.get(i - 1).getDistance();
                    double toDistance = (j == 0) ? 0 : rides.get(j - 1).getDistance();
                    distanceMatrix[i][j] = (int) (fromDistance + toDistance);
                }
            }
        }

        log.debug("📏 Distance Matrix:");
        for (int[] row : distanceMatrix) {
            log.debug(Arrays.toString(row));
        }

        RoutingIndexManager manager = new RoutingIndexManager(numNodes, 1, 0);
        RoutingModel routing = new RoutingModel(manager);

        int transitCallbackIndex = routing.registerTransitCallback((fromIndex, toIndex) -> {
            int fromNode = manager.indexToNode((int) fromIndex);
            int toNode = manager.indexToNode((int) toIndex);
            int cost = distanceMatrix[fromNode][toNode] * 300; // assume 5 min per mile
            log.trace("Transit cost from {} → {} = {}", fromNode, toNode, cost);
            return cost;
        });

        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        routing.addDimension(transitCallbackIndex, 1800, 14400, true, "Time"); // larger buffer for testing
        RoutingDimension timeDimension = routing.getDimensionOrDie("Time");

        long minEpoch = rides.stream()
                .mapToLong(ride -> ride.getPickupTime().toEpochSecond(ZoneOffset.UTC))
                .min().orElse(0);

        long maxEpoch = rides.stream()
                .mapToLong(ride -> ride.getPickupTime().toEpochSecond(ZoneOffset.UTC))
                .max().orElse(0) + 3600;
        for (int i = 0; i < numRides; i++) {
            long startSec = timeDimension.cumulVar(manager.nodeToIndex(i + 1)).min();
            long endSec = timeDimension.cumulVar(manager.nodeToIndex(i + 1)).max();
            log.debug("⏱ Ride {} time window [{} - {}] (seconds)", i + 1, startSec, endSec);
        }
        for (int i = 0; i < numRides; i++) {
            Ride ride = rides.get(i);
            long pickup = ride.getPickupTime().toEpochSecond(ZoneOffset.UTC);
            int waitMinutes = Optional.ofNullable(ride.getWaitTime()).orElse(0);
            long start = Math.max(0, pickup - minEpoch - 7200); // 2hr before
            long end = pickup - minEpoch + 10800 + waitMinutes * 60L; // 3hr after

            log.debug("⏱ Ride {} (pickup={}): time window = [{} - {}] (wait={} min)",
                    i + 1, pickup, start, end, waitMinutes);

            try {
                timeDimension.cumulVar(manager.nodeToIndex(i + 1)).setRange(start, end);
            } catch (Exception e) {
                log.error("❌ Failed to set time window for ride {} → [{} - {}]", i + 1, start, end, e);
            }
        }

        timeDimension.cumulVar(routing.start(0)).setRange(0, maxEpoch - minEpoch + 3600);

        RoutingSearchParameters parameters = newBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(Duration.newBuilder().setSeconds(10))
                .setLogSearch(true)
                .setUseFullPropagation(true)
                .setLocalSearchNeighborhoodOperators(
                        LocalSearchNeighborhoodOperators.newBuilder()
                                .setUseRelocate(BoolValue.of(true))  // ✅ Explicitly set
                                .build()
                )
                .build();

        Assignment solution;
        try {
            solution = routing.solveWithParameters(parameters);
        } catch (Exception e) {
            log.error("❌ OR-Tools solver failed during execution", e);
            return;
        }

        if (solution != null) {
            long index = routing.start(0);
            while (!routing.isEnd(index)) {
                int node = manager.indexToNode((int) index);
                if (node != 0) {
                    rides.get(node - 1).setDriverId(1L); // placeholder assignment
                }
                index = solution.value(routing.nextVar(index));
            }
            rideRepository.saveAll(rides);
            log.info("✅ Optimization complete: {} rides updated", rides.size());
        } else {
            log.warn("❌ No solution found during optimization");
        }
    }
}