//package com.mediroute.integration;
//
//import com.mediroute.entity.Driver;
//import com.mediroute.entity.Patient;
//import com.mediroute.entity.Ride;
//import com.mediroute.entity.Schedule;
//import com.mediroute.repository.DriverRepository;
//import com.mediroute.repository.PatientRepository;
//import com.mediroute.repository.RideRepository;
//import com.mediroute.repository.ScheduleRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//@SpringBootTest
//@ActiveProfiles("test")
//class MediRouteApplicationIntegrationTest {
//
//    @Autowired
//    private DriverRepository driverRepository;
//
//    @Autowired
//    private PatientRepository patientRepository;
//
//    @Autowired
//    private RideRepository rideRepository;
//
//    @Autowired
//    private ScheduleRepository scheduleRepository;
//
//    @BeforeEach
//    void setUp() {
//        driverRepository.deleteAll();
//        patientRepository.deleteAll();
//        rideRepository.deleteAll();
//        scheduleRepository.deleteAll();
//    }
//
//    @Test
//    void contextLoads() {
//        assertNotNull(driverRepository);
//        assertNotNull(patientRepository);
//        assertNotNull(rideRepository);
//        assertNotNull(scheduleRepository);
//    }
//
//    @Test
//    void testSaveAndRetrieveEntities() {
//        Map<String, Object> skills = new HashMap<>();
//        skills.put("wheelchair", true);
//        Driver driver = new Driver();
//        driver.setName("John Doe");
//        driver.setEmail("john.doe@example.com");
//        driver.setPhone("123-456-7890");
//        driver.setVehicleType("van");
//        driver.setSkills(skills);
//        driverRepository.save(driver);
//
//        Map<String, Object> specialNeeds = new HashMap<>();
//        specialNeeds.put("assistance", true);
//        Patient patient = new Patient();
//        patient.setName("Jane Smith");
//        patient.setContactInfo("jane.smith@example.com");
//        patient.setSpecialNeeds(specialNeeds);
//        patientRepository.save(patient);
//
//        Map<String, Object> requiredSkills = new HashMap<>();
//        requiredSkills.put("wheelchair", true);
//        Ride ride = new Ride();
//        ride.setPatient(patient);
//        ride.setPickupLocation("123 Main St");
//        ride.setDropoffLocation("456 Hospital Rd");
//        ride.setPickupTime(LocalDateTime.of(2025, 7, 15, 10, 0)); // 10:00 AM today
//        ride.setWaitTime(5);
//        ride.setIsSequential(false);
//        ride.setDistance(5.5f);
//        ride.setStatus("scheduled");
//        ride.setRequiredVehicleType("van");
//        ride.setRequiredSkills(requiredSkills);
//        rideRepository.save(ride);
//
//        Schedule schedule = new Schedule();
//        schedule.setRide(ride);
//        schedule.setDate(LocalDate.of(2025, 7, 16)); // Tomorrow
//        schedule.setAssignedDriver(driver);
//        scheduleRepository.save(schedule);
//
//        assertTrue(driverRepository.existsByEmail("john.doe@example.com"));
//        assertNotNull(patientRepository.findById(patient.getId()).orElse(null));
//        assertNotNull(rideRepository.findById(ride.getId()).orElse(null));
//        assertNotNull(scheduleRepository.findById(schedule.getId()).orElse(null));
//    }
//}