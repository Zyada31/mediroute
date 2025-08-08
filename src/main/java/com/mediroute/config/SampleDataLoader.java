package com.mediroute.config;

import com.mediroute.dto.DriverDTO;
import com.mediroute.dto.VehicleTypeEnum;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Patient;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.PatientRepository;
import com.mediroute.service.driver.DriverService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SampleDataLoader implements CommandLineRunner {

    private final DriverRepository driverRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (driverRepository.count() == 0) {
            log.info("🚀 Loading sample driver data...");
            createSampleDrivers();
            log.info("✅ Sample data loaded successfully");
        } else {
            log.info("📋 Sample data already exists, skipping...");
        }
    }

    private void createSampleDrivers() {
        // Driver 1: Wheelchair accessible
        Driver driver1 = Driver.builder()
                .name("John Smith")
                .email("john.smith@mediroute.com")
                .phone("303-555-0101")
                .vehicleType(VehicleTypeEnum.WHEELCHAIR_VAN)
                .wheelchairAccessible(true)
                .vehicleCapacity(4)
                .active(true)
                .isTrainingComplete(true)
                .maxDailyRides(8)
                .baseLocation("Denver Medical Center, 1635 Aurora Ct, Aurora, CO 80045")
                .baseLat(39.7392)
                .baseLng(-104.9903)
                .shiftStart(LocalTime.of(6, 0))
                .shiftEnd(LocalTime.of(18, 0))
                .build();

        // Driver 2: Stretcher capable
        Driver driver2 = Driver.builder()
                .name("Maria Rodriguez")
                .email("maria.rodriguez@mediroute.com")
                .phone("303-555-0102")
                .vehicleType(VehicleTypeEnum.STRETCHER_VAN)
                .stretcherCapable(true)
                .wheelchairAccessible(true)
                .vehicleCapacity(2)
                .active(true)
                .isTrainingComplete(true)
                .maxDailyRides(6)
                .baseLocation("University Hospital, 12605 E 16th Ave, Aurora, CO 80045")
                .baseLat(39.7453)
                .baseLng(-104.8378)
                .shiftStart(LocalTime.of(7, 0))
                .shiftEnd(LocalTime.of(19, 0))
                .build();

        // Driver 3: Regular sedan
        Driver driver3 = Driver.builder()
                .name("David Johnson")
                .email("david.johnson@mediroute.com")
                .phone("303-555-0103")
                .vehicleType(VehicleTypeEnum.SEDAN)
                .vehicleCapacity(4)
                .active(true)
                .isTrainingComplete(true)
                .maxDailyRides(10)
                .baseLocation("Presbyterian/St. Joseph Hospital, 1719 E 19th Ave, Denver, CO 80218")
                .baseLat(39.7470)
                .baseLng(-104.9741)
                .shiftStart(LocalTime.of(8, 0))
                .shiftEnd(LocalTime.of(17, 0))
                .build();

        // Save all drivers
        driverRepository.save(driver1);
        driverRepository.save(driver2);
        driverRepository.save(driver3);

        log.info("✅ Created {} sample drivers", 3);
    }
}