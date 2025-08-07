package com.mediroute.repository;

import com.mediroute.entity.AssignmentAudit;
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
@Tag(name = "Assignment Audit Repository", description = "Assignment audit data access operations")
public interface AssignmentAuditRepository extends BaseRepository<AssignmentAudit, Long> {

    // Basic Queries
    List<AssignmentAudit> findByAssignmentDateOrderByAssignmentTimeDesc(LocalDate assignmentDate);

    Optional<AssignmentAudit> findByBatchId(String batchId);

    List<AssignmentAudit> findByBatchIdContaining(String batchIdPattern);

    // Date Range Queries
    List<AssignmentAudit> findByAssignmentTimeBetweenOrderByAssignmentTimeDesc(LocalDateTime start, LocalDateTime end);

    List<AssignmentAudit> findByAssignmentDateBetweenOrderByAssignmentDateDesc(LocalDate startDate, LocalDate endDate);

    // Performance Queries
    @Query("SELECT a FROM AssignmentAudit a WHERE a.successRate >= :minSuccessRate ORDER BY a.successRate DESC")
    List<AssignmentAudit> findByMinSuccessRate(@Param("minSuccessRate") Double minSuccessRate);

    @Query("SELECT a FROM AssignmentAudit a WHERE a.totalRides >= :minRides ORDER BY a.assignmentTime DESC")
    List<AssignmentAudit> findByMinRideCount(@Param("minRides") Integer minRides);

    // Statistics
    @Query("SELECT AVG(a.successRate) FROM AssignmentAudit a WHERE a.assignmentDate BETWEEN :startDate AND :endDate")
    Double getAverageSuccessRateForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(a.totalRides) FROM AssignmentAudit a WHERE a.assignmentDate = :date")
    Long getTotalRidesForDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(a.assignedRides) FROM AssignmentAudit a WHERE a.assignmentDate = :date")
    Long getAssignedRidesForDate(@Param("date") LocalDate date);

    // Recent Records
    @Query("SELECT a FROM AssignmentAudit a ORDER BY a.assignmentTime DESC")
    List<AssignmentAudit> findAllOrderByAssignmentTimeDesc();

    @Query("SELECT a FROM AssignmentAudit a WHERE a.assignmentTime >= :since ORDER BY a.assignmentTime DESC")
    List<AssignmentAudit> findRecentAudits(@Param("since") LocalDateTime since);
}
