// 6. RideAudit Repository
package com.mediroute.repository;

import com.mediroute.entity.RideAudit;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Tag(name = "Ride Audit Repository", description = "Ride audit trail data access operations")
public interface RideAuditRepository extends BaseRepository<RideAudit, Long> {

    // Basic Queries
    List<RideAudit> findByRideIdOrderByChangedAtDesc(Long rideId);

    List<RideAudit> findByRideIdAndFieldNameOrderByChangedAtDesc(Long rideId, String fieldName);

    List<RideAudit> findByChangedByOrderByChangedAtDesc(String changedBy);

    // Time-based Queries
    List<RideAudit> findByChangedAtBetweenOrderByChangedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT ra FROM RideAudit ra WHERE ra.ride.id = :rideId AND ra.changedAt BETWEEN :start AND :end " +
            "ORDER BY ra.changedAt DESC")
    List<RideAudit> findByRideIdAndChangedAtBetween(@Param("rideId") Long rideId,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    // Field-specific Queries
    List<RideAudit> findByFieldNameOrderByChangedAtDesc(String fieldName);

    @Query("SELECT DISTINCT ra.fieldName FROM RideAudit ra ORDER BY ra.fieldName")
    List<String> findDistinctFieldNames();

    // System vs Manual Changes
    List<RideAudit> findBySystemGeneratedTrueOrderByChangedAtDesc();

    List<RideAudit> findBySystemGeneratedFalseOrderByChangedAtDesc();

    // Statistics
    @Query("SELECT COUNT(ra) FROM RideAudit ra WHERE ra.ride.id = :rideId")
    long countAuditEntriesForRide(@Param("rideId") Long rideId);

    @Query("SELECT ra.fieldName, COUNT(ra) FROM RideAudit ra GROUP BY ra.fieldName ORDER BY COUNT(ra) DESC")
    List<Object[]> countChangesByField();

    @Query("SELECT ra.changedBy, COUNT(ra) FROM RideAudit ra WHERE ra.systemGenerated = false " +
            "GROUP BY ra.changedBy ORDER BY COUNT(ra) DESC")
    List<Object[]> countChangesByUser();
}
