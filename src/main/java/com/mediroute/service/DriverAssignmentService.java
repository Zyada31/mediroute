package com.mediroute.service;

import com.mediroute.entity.Driver;
import com.mediroute.entity.Ride;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverAssignmentService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    public void assignDriversForDate(LocalDateTime from, LocalDateTime to) {
        List<Ride> rides = rideRepository.findByPickupTimeBetween(from, to)
                .stream().filter(ride -> ride.getDriver() == null).toList();
        List<Driver> drivers = driverRepository.findByActiveTrue();
        Map<Long, Integer> rideCountMap = new HashMap<>();

        for (Ride ride : rides) {
            log.info("🚗 Attempting assignment for Ride {} at {} | needs: {} | skills: {}",
                    ride.getId(), ride.getPickupTime(), ride.getRequiredVehicleType(), ride.getRequiredSkills());

            boolean assigned = false;

            for (Driver driver : drivers) {
                boolean shiftOk = isWithinShift(driver, ride.getPickupTime());
                boolean vehicleOk = matchesVehicle(driver, ride);
                boolean skillOk = hasRequiredSkills(driver, ride);
                boolean capacityOk = hasCapacity(driver, rideCountMap);

                log.debug("🔍 Driver {} Check for Ride {} => shiftOk={}, vehicleOk={}, skillOk={}, capacityOk={}",
                        driver.getName(), ride.getId(), shiftOk, vehicleOk, skillOk, capacityOk);

                if (shiftOk && vehicleOk && skillOk && capacityOk) {
                    ride.setDriver(driver);
                    rideCountMap.merge(driver.getId(), 1, Integer::sum);
                    log.info("✅ Assigned Driver '{}' to Ride {}", driver.getName(), ride.getId());
                    assigned = true;
                    break;
                }
            }

            if (!assigned) {
                log.warn("❌ No eligible driver found for Ride {}", ride.getId());
            }
        }

        rideRepository.saveAll(rides);
        log.info("🔄 Assignment complete. Summary (driverId → rideCount): {}", rideCountMap);
    }

    private boolean isWithinShift(Driver driver, LocalDateTime time) {
        return (driver.getShiftStart() == null || !time.isBefore(driver.getShiftStart())) &&
                (driver.getShiftEnd() == null || !time.isAfter(driver.getShiftEnd()));
    }

    private boolean matchesVehicle(Driver driver, Ride ride) {
        return Objects.equals(driver.getVehicleType(), ride.getRequiredVehicleType());
    }

    private boolean hasRequiredSkills(Driver driver, Ride ride) {
        List<String> required = Optional.ofNullable(ride.getRequiredSkills()).orElse(List.of());

        Map<String, Boolean> driverSkills = Optional.ofNullable(driver.getSkills())
                .orElse(Map.of())
                .entrySet().stream()
                .filter(e -> e.getValue() instanceof Boolean)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Boolean) e.getValue()));

        if (driver.getSkills() != null && driver.getSkills().values().stream().anyMatch(v -> !(v instanceof Boolean))) {
            log.warn("⚠️ Driver {} has invalid skill entries: {}", driver.getId(), driver.getSkills());
        }

        return required.stream()
                .allMatch(skill -> Boolean.TRUE.equals(driverSkills.get(skill)));
    }

    private boolean hasCapacity(Driver driver, Map<Long, Integer> rideCountMap) {
        return rideCountMap.getOrDefault(driver.getId(), 0)
                < Optional.ofNullable(driver.getMaxDailyRides()).orElse(8);
    }
}