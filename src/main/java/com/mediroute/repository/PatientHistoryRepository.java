// 8. PatientHistory Repository
package com.mediroute.repository;

import com.mediroute.dto.RideStatus;
import com.mediroute.entity.PatientHistory;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Tag(name = "Patient History Repository", description = "Patient history data access operations")
public interface PatientHistoryRepository extends BaseRepository<PatientHistory, Long> {

    // Basic Queries
    List<PatientHistory> findByPatientIdOrderByRideDateDesc(Long patientId);

    List<PatientHistory> findByPatientIdAndRideStatusOrderByRideDateDesc(Long patientId, RideStatus rideStatus);

    List<PatientHistory> findByRideIdOrderByRideDateDesc(Long rideId);

    // Date Range Queries
    @Query("SELECT ph FROM PatientHistory ph WHERE ph.patient.id = :patientId AND " +
            "ph.rideDate BETWEEN :startDate AND :endDate ORDER BY ph.rideDate DESC")
    List<PatientHistory> findByPatientAndDateRange(@Param("patientId") Long patientId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ph FROM PatientHistory ph WHERE ph.rideDate BETWEEN :startDate AND :endDate " +
            "ORDER BY ph.rideDate DESC")
    List<PatientHistory> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // Performance Queries
    @Query("SELECT ph FROM PatientHistory ph WHERE ph.patient.id = :patientId AND ph.onTimePickup = true " +
            "AND ph.onTimeDropoff = true ORDER BY ph.rideDate DESC")
    List<PatientHistory> findOnTimeRidesForPatient(@Param("patientId") Long patientId);

    @Query("SELECT ph FROM PatientHistory ph WHERE ph.patientSatisfaction >= :minSatisfaction " +
            "ORDER BY ph.patientSatisfaction DESC, ph.rideDate DESC")
    List<PatientHistory> findByMinSatisfactionScore(@Param("minSatisfaction") Integer minSatisfaction);

    // Statistics
    @Query("SELECT AVG(ph.distance) FROM PatientHistory ph WHERE ph.patient.id = :patientId")
    Double getAverageDistanceForPatient(@Param("patientId") Long patientId);

    @Query("SELECT AVG(ph.durationMinutes) FROM PatientHistory ph WHERE ph.patient.id = :patientId")
    Double getAverageDurationForPatient(@Param("patientId") Long patientId);

    @Query("SELECT COUNT(ph) FROM PatientHistory ph WHERE ph.patient.id = :patientId AND ph.rideStatus = 'COMPLETED'")
    long countCompletedRidesForPatient(@Param("patientId") Long patientId);

    @Query("SELECT COUNT(ph) FROM PatientHistory ph WHERE ph.patient.id = :patientId AND " +
            "ph.onTimePickup = true AND ph.onTimeDropoff = true")
    long countOnTimeRidesForPatient(@Param("patientId") Long patientId);

    // Recent Activity
    @Query("SELECT ph FROM PatientHistory ph WHERE ph.patient.id = :patientId " +
            "ORDER BY ph.rideDate DESC LIMIT :limit")
    List<PatientHistory> findRecentRidesForPatient(@Param("patientId") Long patientId, @Param("limit") int limit);
}
