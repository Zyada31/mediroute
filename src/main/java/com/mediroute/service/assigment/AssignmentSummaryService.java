package com.mediroute.service.assigment;

import com.mediroute.dto.DriverRideSummary;
import com.mediroute.entity.Ride;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentSummaryService {

    private final RideRepository rideRepository;

    public List<DriverRideSummary> getSummaryForDate(LocalDateTime dayStart, LocalDateTime dayEnd) {
        // Get all rides in the time range that are assigned
        List<Ride> assignedRides = rideRepository.findByPickupTimeBetweenWithDriversAndPatient(dayStart, dayEnd)
                .stream()
                .filter(ride -> ride.getPickupDriver() != null || ride.getDropoffDriver() != null)
                .toList();

//        if (assignedRides.isEmpty()) {
//            log.info("No assigned rides found between {} and {}", dayStart, dayEnd);
//            return new ArrayList<>();
//        }

        // Group by pickup driver (primary assignment)
        Map<Long, List<Ride>> ridesByDriver = assignedRides.stream()
                .filter(ride -> ride.getPickupDriver() != null)
                .collect(Collectors.groupingBy(ride -> ride.getPickupDriver().getId()));

        // Create summaries
        return ridesByDriver.entrySet().stream()
                .map(entry -> {
                    Long driverId = entry.getKey();
                    List<Ride> driverRides = entry.getValue();

                    // Get driver info from first ride
                    String driverName = driverRides.get(0).getPickupDriver().getName();
                    String vehicleType = driverRides.get(0).getPickupDriver().getVehicleType().toString();

                    List<Long> rideIds = driverRides.stream()
                            .map(Ride::getId)
                            .collect(Collectors.toList());

                    Double totalDistance = driverRides.stream()
                            .mapToDouble(ride -> ride.getDistance() != null ? ride.getDistance() : 0.0)
                            .sum();

                    boolean medicalCapable = driverRides.get(0).getPickupDriver().getWheelchairAccessible() ||
                            driverRides.get(0).getPickupDriver().getStretcherCapable() ||
                            driverRides.get(0).getPickupDriver().getOxygenEquipped();

                    return DriverRideSummary.builder()
                            .driverId(driverId)
                            .driverName(driverName)
                            .date(dayStart.toLocalDate())
                            .totalRides(driverRides.size())
                            .completedRides((int) driverRides.stream()
                                    .filter(ride -> ride.getStatus().name().equals("COMPLETED"))
                                    .count())
                            .pendingRides((int) driverRides.stream()
                                    .filter(ride -> !ride.getStatus().name().equals("COMPLETED") &&
                                            !ride.getStatus().name().equals("CANCELLED"))
                                    .count())
                            .cancelledRides((int) driverRides.stream()
                                    .filter(ride -> ride.getStatus().name().equals("CANCELLED"))
                                    .count())
                            .rideIds(rideIds)
                            .totalDistance(totalDistance)
                            .vehicleType(vehicleType)
                            .medicalTransportCapable(medicalCapable)
                            .build();
                })
                .collect(Collectors.toList());
    }
}