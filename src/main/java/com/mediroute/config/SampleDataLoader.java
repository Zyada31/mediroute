package com.mediroute.config;

import com.mediroute.dto.DriverDTO;
import com.mediroute.entity.Patient;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.PatientRepository;
import com.mediroute.service.driver.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SampleDataLoader implements CommandLineRunner {

    private final DriverRepository driverRepository;
    private final PatientRepository patientRepository;
    private final DriverService driverService;

    @Override
    public void run(String... args) throws Exception {
        if (driverRepository.count() == 0) {
            loadSampleData();
        }
    }

    private void loadSampleData() {
        log.info("🔄 Loading sample data for testing...");

        try {
            // Create sample drivers
            createSampleDrivers();

            // Create sample patients
            createSamplePatients();

            log.info("✅ Sample data loaded successfully");
        } catch (Exception e) {
            log.error("❌ Failed to load sample data: {}", e.getMessage(), e);
        }
    }

    private void createSampleDrivers() {
        // Driver 1 - Wheelchair accessible
        DriverDTO driver1 = new DriverDTO();
        driver1.setName("John Smith");
        driver1.setEmail("john.smith@mediroute.com");
        driver1.setPhone("+1234567890");
        driver1.setVehicleType("WHEELCHAIR_VAN");
        driver1.setWheelchairAccessible(true);
        driver1.setStretcherCapable(false);
        driver1.setOxygenEquipped(false);
        driver1.setBaseLocation("123 Main St, Denver, CO 80202");
        driver1.setMaxDailyRides(8);
        driver1.setIsTrainingComplete(true);

        Map<String, Boolean> skills1 = new HashMap<>();
        skills1.put("CPR", true);
        skills1.put("First Aid", true);
        driver1.setSkills(skills1);

        try {
            driverService.createOrUpdateDriver(driver1, false);
            log.info("✅ Created driver: {}", driver1.getName());
        } catch (Exception e) {
            log.warn("⚠️ Driver {} already exists", driver1.getName());
        }

        // Driver 2 - Stretcher capable
        DriverDTO driver2 = new DriverDTO();
        driver2.setName("Jane Doe");
        driver2.setEmail("jane.doe@mediroute.com");
        driver2.setPhone("+1987654321");
        driver2.setVehicleType("STRETCHER_VAN");
        driver2.setWheelchairAccessible(true);
        driver2.setStretcherCapable(true);
        driver2.setOxygenEquipped(true);
        driver2.setBaseLocation("456 Oak Ave, Denver, CO 80203");
        driver2.setMaxDailyRides(6);
        driver2.setIsTrainingComplete(true);

        Map<String, Boolean> skills2 = new HashMap<>();
        skills2.put("CPR", true);
        skills2.put("First Aid", true);
        skills2.put("Medical Transport", true);
        driver2.setSkills(skills2);

        try {
            driverService.createOrUpdateDriver(driver2, false);
            log.info("✅ Created driver: {}", driver2.getName());
        } catch (Exception e) {
            log.warn("⚠️ Driver {} already exists", driver2.getName());
        }

        // Driver 3 - Standard sedan
        DriverDTO driver3 = new DriverDTO();
        driver3.setName("Mike Johnson");
        driver3.setEmail("mike.johnson@mediroute.com");
        driver3.setPhone("+1555123456");
        driver3.setVehicleType("SEDAN");
        driver3.setWheelchairAccessible(false);
        driver3.setStretcherCapable(false);
        driver3.setOxygenEquipped(false);
        driver3.setBaseLocation("789 Pine St, Denver, CO 80204");
        driver3.setMaxDailyRides(10);
        driver3.setIsTrainingComplete(true);

        try {
            driverService.createOrUpdateDriver(driver3, false);
            log.info("✅ Created driver: {}", driver3.getName());
        } catch (Exception e) {
            log.warn("⚠️ Driver {} already exists", driver3.getName());
        }
    }

    private void createSamplePatients() {
        // Patient 1 - Wheelchair user
        Patient patient1 = Patient.builder()
                .name("Alice Williams")
                .phone("+1111222333")
                .email("alice.williams@email.com")
                .requiresWheelchair(true)
                .requiresStretcher(false)
                .requiresOxygen(false)
                .emergencyContactName("Bob Williams")
                .emergencyContactPhone("+1111222334")
                .insuranceProvider("Medicare")
                .isActive(true)
                .build();

        try {
            patientRepository.save(patient1);
            log.info("✅ Created patient: {}", patient1.getName());
        } catch (Exception e) {
            log.warn("⚠️ Patient {} may already exist", patient1.getName());
        }

        // Patient 2 - Stretcher required
        Patient patient2 = Patient.builder()
                .name("Robert Brown")
                .phone("+1222333444")
                .email("robert.brown@email.com")
                .requiresWheelchair(false)
                .requiresStretcher(true)
                .requiresOxygen(false)
                .emergencyContactName("Mary Brown")
                .emergencyContactPhone("+1222333445")
                .insuranceProvider("Blue Cross")
                .isActive(true)
                .build();

        try {
            patientRepository.save(patient2);
            log.info("✅ Created patient: {}", patient2.getName());
        } catch (Exception e) {
            log.warn("⚠️ Patient {} may already exist", patient2.getName());
        }

        // Patient 3 - Standard transport
        Patient patient3 = Patient.builder()
                .name("Sarah Davis")
                .phone("+1333444555")
                .email("sarah.davis@email.com")
                .requiresWheelchair(false)
                .requiresStretcher(false)
                .requiresOxygen(false)
                .emergencyContactName("Tom Davis")
                .emergencyContactPhone("+1333444556")
                .insuranceProvider("Aetna")
                .isActive(true)
                .build();

        try {
            patientRepository.save(patient3);
            log.info("✅ Created patient: {}", patient3.getName());
        } catch (Exception e) {
            log.warn("⚠️ Patient {} may already exist", patient3.getName());
        }
    }
}