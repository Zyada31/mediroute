//package com.mediroute;
//
//import com.mediroute.entity.Driver;
//import com.mediroute.entity.Patient;
//import com.mediroute.entity.Ride;
//import com.mediroute.service.driver.DriverService;
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.annotation.Rollback;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//
//@SpringBootTest
//@Transactional
//@Rollback
//public class LazyLoadingTest {
//
//    @Autowired
//    private TestEntityManager em;
//
//    @Test
//    public void testAllEntitiesCanBeFullyLoaded() {
//        // Create test data
//        Driver driver = createTestDriver();
//        Patient patient = createTestPatient();
//        Ride ride = createTestRide(driver, patient);
//
//        em.persist(driver);
//        em.persist(patient);
//        em.persist(ride);
//        em.flush();
//        em.clear();
//
//        // Load and access all relationships outside transaction
//        Ride loadedRide = em.find(Ride.class, ride.getId());
//        em.detach(loadedRide);
//
//        // This should NOT throw LazyInitializationException
//        assertDoesNotThrow(() -> {
//            loadedRide.getPatient().getName();
//            loadedRide.getPickupDriver().getName();
//            loadedRide.getDropoffDriver().getName();
//        });
//    }
//}
