

package com.mediroute.repository;

import com.mediroute.entity.Patient;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends BaseRepository<Patient, Long> {

    Optional<Patient> findByPhone(String phone);
    Optional<Patient> findByPhoneAndOrgId(String phone, Long orgId);
    Optional<Patient> findByNameAndPhone(String name, String phone);
    List<Patient> findByIsActiveTrue();
    List<Patient> findByNameContainingIgnoreCase(String name);

    // Medical Requirements
    List<Patient> findByRequiresWheelchairTrue();
    List<Patient> findByRequiresStretcherTrue();
    List<Patient> findByRequiresOxygenTrue();

    @Query("SELECT p FROM Patient p WHERE p.requiresWheelchair = true OR p.requiresStretcher = true OR p.requiresOxygen = true")
    List<Patient> findPatientsWithSpecialNeeds();

    @Query("SELECT p FROM Patient p WHERE p.insuranceProvider = :provider AND p.isActive = true")
    List<Patient> findByInsuranceProvider(@Param("provider") String provider);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.requiresWheelchair = true AND p.isActive = true")
    long countActiveWheelchairPatients();

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.requiresStretcher = true AND p.isActive = true")
    long countActiveStretcherPatients();

    @Query("SELECT p FROM Patient p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:phone IS NULL OR p.phone LIKE CONCAT('%', :phone, '%')) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive)")
    List<Patient> findPatientsWithCriteria(@Param("name") String name,
                                           @Param("phone") String phone,
                                           @Param("isActive") Boolean isActive);
}