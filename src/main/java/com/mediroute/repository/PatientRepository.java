// 2. Enhanced Patient Repository
package com.mediroute.repository;

import com.mediroute.entity.Patient;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Tag(name = "Patient Repository", description = "Patient data access operations")
public interface PatientRepository extends BaseRepository<Patient, Long> {

    // Basic Queries
    Optional<Patient> findByPhone(String phone);

    Optional<Patient> findByNameAndPhone(String name, String phone);

    List<Patient> findByIsActiveTrue();

    List<Patient> findByNameContainingIgnoreCase(String name);

    // Medical Requirements
    List<Patient> findByRequiresWheelchairTrue();

    List<Patient> findByRequiresStretcherTrue();

    List<Patient> findByRequiresOxygenTrue();

    // Complex Queries
    @Query("SELECT p FROM Patient p WHERE p.requiresWheelchair = true OR p.requiresStretcher = true OR p.requiresOxygen = true")
    List<Patient> findPatientsWithSpecialNeeds();

    @Query("SELECT DISTINCT p FROM Patient p JOIN p.rides r WHERE r.pickupTime BETWEEN :startDate AND :endDate")
    List<Patient> findPatientsWithRidesInDateRange(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Patient p WHERE p.insuranceProvider = :provider AND p.isActive = true")
    List<Patient> findByInsuranceProvider(@Param("provider") String provider);

    // Statistics
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.requiresWheelchair = true AND p.isActive = true")
    long countActiveWheelchairPatients();

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.requiresStretcher = true AND p.isActive = true")
    long countActiveStretcherPatients();

    // Advanced Search
    @Query("SELECT p FROM Patient p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:phone IS NULL OR p.phone LIKE CONCAT('%', :phone, '%')) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive)")
    List<Patient> findPatientsWithCriteria(@Param("name") String name,
                                           @Param("phone") String phone,
                                           @Param("isActive") Boolean isActive);
}