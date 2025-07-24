package com.mediroute.repository;

import com.mediroute.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByNameAndContactInfo(String name, String contactInfo);
}