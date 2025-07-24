package com.mediroute.repository;

import com.mediroute.entity.PatientHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientHistoryRepository extends JpaRepository<PatientHistory, Long> {
}