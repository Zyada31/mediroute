package com.mediroute.integration;

import com.mediroute.dto.RideStatus;
import com.mediroute.dto.VehicleTypeEnum;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.entity.embeddable.Location;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.PatientRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.ride.EnhancedMedicalTransportOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class SyntheticDataTest {

    @Autowired DriverRepository driverRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired RideRepository rideRepository;
    @Autowired EnhancedMedicalTransportOptimizer optimizer;

    private static final Random RNG = new Random(42);

    @BeforeEach
    @Transactional
    void clean() {
        rideRepository.deleteAll();
        patientRepository.deleteAll();
        driverRepository.deleteAll();
    }
/*
Optimizer runs end-to-end on a dataset of 20 drivers and 200 rides without errors.
Basic performance is acceptable locally (under 10s as asserted).
Some assignments occur (assignment rate > 0%), so constraints and scoring aren’t blocking everything.
DB mappings and required fields are satisfied; schema creates cleanly in tests.
* */
    @Test
    @Transactional
    void generateData_and_runOptimizer_performance_and_accuracy_checks() {
        long orgId = 1L;
        int driverCount = 20;
        int rideCount = 200;

        // Seed drivers around Denver
        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < driverCount; i++) {
            Driver d = new Driver();
            d.setName("Driver " + (i + 1));
            d.setPhone("+1303444" + String.format("%04d", i));
            d.setActive(true);
            d.setIsTrainingComplete(true);
            // Mix capabilities
            if (i % 10 == 0) {
                d.setVehicleType(VehicleTypeEnum.WHEELCHAIR_VAN);
                d.setWheelchairAccessible(true);
            } else if (i % 10 == 1) {
                d.setVehicleType(VehicleTypeEnum.STRETCHER_VAN);
                d.setStretcherCapable(true);
                d.setOxygenEquipped(true);
            } else {
                d.setVehicleType(VehicleTypeEnum.SEDAN);
            }
            d.setBaseLocation("Denver, CO");
            d.setBaseLat(randomInRange(39.60, 39.90));
            d.setBaseLng(randomInRange(-105.10, -104.60));
            d.setOrgId(orgId);
            drivers.add(driverRepository.save(d));
        }

        // Seed patients
        List<Patient> patients = new ArrayList<>();
        for (int i = 0; i < rideCount; i++) {
            Patient p = new Patient();
            p.setName("Patient " + (i + 1));
            p.setPhone("+1303555" + String.format("%04d", i));
            // 10% wheelchair, 5% stretcher
            if (i % 10 == 0) p.setRequiresWheelchair(true);
            if (i % 20 == 0) p.setRequiresStretcher(true);
            p.setOrgId(orgId);
            patients.add(patientRepository.save(p));
        }

        // Seed rides during 8am-4pm windows
        List<Ride> rides = new ArrayList<>();
        for (int i = 0; i < rideCount; i++) {
            Ride r = new Ride();
            r.setPatient(patients.get(i));
            r.setPickupTime(LocalDateTime.now().withHour(8 + (i % 8)).withMinute(15).withSecond(0).withNano(0));
            r.setPickupLocation(new Location("pickup-" + i, randomInRange(39.60, 39.90), randomInRange(-105.10, -104.60)));
            r.setDropoffLocation(new Location("drop-" + i, randomInRange(39.60, 39.90), randomInRange(-105.10, -104.60)));
            r.setOrgId(orgId);
            r.setStatus(RideStatus.SCHEDULED);
            rides.add(rideRepository.save(r));
        }

        // Measure performance
        long start = System.nanoTime();
        var result = optimizer.performMedicalTransportOptimization(rides, drivers, "TEST_BATCH");
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        int assigned = result.getAssignedRideCount();
        int total = rideCount;
        double assignmentRate = total == 0 ? 0.0 : (assigned * 100.0 / total);

        // Accuracy sanity: some assignments should occur
        assertThat(assigned).isGreaterThan(0);
        // Performance sanity: keep under a lenient bound for local/dev
        assertThat(elapsedMs).isLessThan(10_000);

        // Build rideId -> driverId map for assigned rides
        Map<Long, Long> rideToDriver = new HashMap<>();
        result.getDriverAssignments().forEach((driverId, rideIds) -> {
            for (Long rideId : rideIds) rideToDriver.put(rideId, driverId);
        });

        // Capability checks: wheelchair rides -> wheelchairAccessible; stretcher -> stretcherCapable
        List<Ride> wheelchairRides = rides.stream().filter(r -> Boolean.TRUE.equals(r.getPatient().getRequiresWheelchair())).collect(Collectors.toList());
        for (Ride r : wheelchairRides) {
            Long did = rideToDriver.get(r.getId());
            if (did != null) {
                Driver d = drivers.stream().filter(dr -> dr.getId().equals(did)).findFirst().orElseThrow();
                assertThat(Boolean.TRUE.equals(d.getWheelchairAccessible()))
                        .as("Wheelchair ride %s assigned to wheelchair-capable driver", r.getId())
                        .isTrue();
            }
        }
        List<Ride> stretcherRides = rides.stream().filter(r -> Boolean.TRUE.equals(r.getPatient().getRequiresStretcher())).collect(Collectors.toList());
        for (Ride r : stretcherRides) {
            Long did = rideToDriver.get(r.getId());
            if (did != null) {
                Driver d = drivers.stream().filter(dr -> dr.getId().equals(did)).findFirst().orElseThrow();
                assertThat(Boolean.TRUE.equals(d.getStretcherCapable()))
                        .as("Stretcher ride %s assigned to stretcher-capable driver", r.getId())
                        .isTrue();
            }
        }

        // Distribution sanity: more than one driver assigned
        assertThat(result.getAssignedDriverIds().size()).isGreaterThan(1);

        // Deadhead sanity (haversine base->pickup for assigned rides)
        List<Double> deadheadsKm = new ArrayList<>();
        for (Map.Entry<Long, Long> e : rideToDriver.entrySet()) {
            Ride r = rides.stream().filter(rr -> rr.getId().equals(e.getKey())).findFirst().orElse(null);
            Driver d = drivers.stream().filter(dr -> dr.getId().equals(e.getValue())).findFirst().orElse(null);
            if (r != null && d != null && r.getPickupLocation() != null && r.getPickupLocation().isValid()) {
                deadheadsKm.add(haversineKm(d.getBaseLat(), d.getBaseLng(), r.getPickupLocation().getLatitude(), r.getPickupLocation().getLongitude()));
            }
        }
        if (!deadheadsKm.isEmpty()) {
            double avg = deadheadsKm.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double max = deadheadsKm.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            assertThat(avg).isLessThan(30.0);
            assertThat(max).isLessThan(100.0);
        }

        // Unassigned reasons present if any unassigned
        int unassignedCount = total - assigned;
        if (unassignedCount > 0) {
            assertThat(result.getUnassignedReasons()).isNotEmpty();
        }

        // Print quick summary for visibility in CI logs
        System.out.printf("Synthetic optimizer: assigned=%d/%d (%.1f%%), elapsed=%dms, drivers=%d, deadheadAvg<30=%b%n",
                assigned, total, assignmentRate, elapsedMs, result.getAssignedDriverIds().size(),
                deadheadsKm.isEmpty() || deadheadsKm.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) < 30.0);
    }

    private static double randomInRange(double min, double max) {
        return min + (max - min) * RNG.nextDouble();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
