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
    List<AssignmentAudit> findByOrgIdAndAssignmentDateOrderByAssignmentTimeDesc(Long orgId, LocalDate assignmentDate);

    Optional<AssignmentAudit> findByOrgIdAndBatchId(Long orgId, String batchId);

    List<AssignmentAudit> findByOrgIdAndBatchIdContaining(Long orgId, String batchIdPattern);

    // Date Range Queries
    List<AssignmentAudit> findByOrgIdAndAssignmentTimeBetweenOrderByAssignmentTimeDesc(Long orgId, LocalDateTime start, LocalDateTime end);

    List<AssignmentAudit> findByOrgIdAndAssignmentDateBetweenOrderByAssignmentDateDesc(Long orgId, LocalDate startDate, LocalDate endDate);

    // Performance Queries
    @Query("SELECT a FROM AssignmentAudit a WHERE a.orgId = :orgId AND a.successRate >= :minSuccessRate ORDER BY a.successRate DESC")
    List<AssignmentAudit> findByMinSuccessRate(@Param("orgId") Long orgId, @Param("minSuccessRate") Double minSuccessRate);

    @Query("SELECT a FROM AssignmentAudit a WHERE a.orgId = :orgId AND a.totalRides >= :minRides ORDER BY a.assignmentTime DESC")
    List<AssignmentAudit> findByMinRideCount(@Param("orgId") Long orgId, @Param("minRides") Integer minRides);

    // Statistics
    @Query("SELECT AVG(a.successRate) FROM AssignmentAudit a WHERE a.orgId = :orgId AND a.assignmentDate BETWEEN :startDate AND :endDate")
    Double getAverageSuccessRateForDateRange(@Param("orgId") Long orgId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(a.totalRides) FROM AssignmentAudit a WHERE a.orgId = :orgId AND a.assignmentDate = :date")
    Long getTotalRidesForDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    @Query("SELECT SUM(a.assignedRides) FROM AssignmentAudit a WHERE a.orgId = :orgId AND a.assignmentDate = :date")
    Long getAssignedRidesForDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    // Recent Records
    @Query("SELECT a FROM AssignmentAudit a WHERE a.orgId = :orgId ORDER BY a.assignmentTime DESC")
    List<AssignmentAudit> findAllOrderByAssignmentTimeDesc(@Param("orgId") Long orgId);

    @Query("SELECT a FROM AssignmentAudit a WHERE a.orgId = :orgId AND a.assignmentTime >= :since ORDER BY a.assignmentTime DESC")
    List<AssignmentAudit> findRecentAudits(@Param("orgId") Long orgId, @Param("since") LocalDateTime since);
}
