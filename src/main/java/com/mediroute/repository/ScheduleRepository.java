package com.mediroute.repository;

import com.mediroute.dto.RideStatus;
import com.mediroute.entity.Schedule;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Tag(name = "Schedule Repository", description = "Schedule data access operations")
public interface ScheduleRepository extends BaseRepository<Schedule, Long> {

    // Basic Queries
    List<Schedule> findByDateOrderBySequenceNumber(LocalDate date);

    List<Schedule> findByAssignedDriverIdAndDateOrderBySequenceNumber(Long driverId, LocalDate date);

    Optional<Schedule> findByRideIdAndDate(Long rideId, LocalDate date);

    // Status-based Queries
    List<Schedule> findByDateAndStatusOrderBySequenceNumber(LocalDate date, RideStatus status);

    List<Schedule> findByAssignedDriverIdAndDateAndStatus(Long driverId, LocalDate date, RideStatus status);

    // Time-based Queries - SIMPLIFIED
    @Query("SELECT s FROM Schedule s WHERE s.date BETWEEN :startDate AND :endDate ORDER BY s.date, s.sequenceNumber")
    List<Schedule> findByDateRangeOrderByDateAndSequence(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.assignedDriver.id = :driverId AND s.date BETWEEN :startDate AND :endDate " +
            "ORDER BY s.date, s.sequenceNumber")
    List<Schedule> findByDriverAndDateRange(@Param("driverId") Long driverId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // Optimization Queries - SIMPLIFIED
    List<Schedule> findByOptimizationBatchIdOrderBySequenceNumber(String batchId);

    @Query("SELECT s FROM Schedule s WHERE s.optimizationBatchId = :batchId AND s.assignedDriver.id = :driverId " +
            "ORDER BY s.sequenceNumber")
    List<Schedule> findByBatchIdAndDriverOrderBySequence(@Param("batchId") String batchId, @Param("driverId") Long driverId);

    // Performance Queries - SIMPLIFIED (remove time arithmetic that might cause issues)
    @Query("SELECT s FROM Schedule s WHERE s.actualStartTime IS NOT NULL AND s.estimatedStartTime IS NOT NULL " +
            "AND s.date = :date")
    List<Schedule> findSchedulesWithActualTimes(@Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.assignedDriver.id = :driverId AND s.date = :date")
    long countSchedulesForDriverAndDate(@Param("driverId") Long driverId, @Param("date") LocalDate date);

    // Statistics - SIMPLIFIED (remove complex time calculations)
    @Query("SELECT s.assignedDriver.id, COUNT(s) FROM Schedule s WHERE s.date = :date GROUP BY s.assignedDriver.id")
    List<Object[]> countSchedulesByDriverForDate(@Param("date") LocalDate date);

    // Additional simplified queries
    List<Schedule> findByDateAndAssignedDriverIdIsNotNull(LocalDate date);

    List<Schedule> findByDateAndAssignedDriverIdIsNull(LocalDate date);
}