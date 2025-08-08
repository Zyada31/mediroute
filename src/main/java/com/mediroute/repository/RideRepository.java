// 1. Enhanced RideRepository with JOIN FETCH
package com.mediroute.repository;

import com.mediroute.dto.Priority;
import com.mediroute.dto.RideStatus;
import com.mediroute.dto.RideType;
import com.mediroute.entity.Ride;
import com.mediroute.repository.base.BaseRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends BaseRepository<Ride, Long> {

    // ========== FIXED LAZY LOADING QUERIES ==========

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "WHERE r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.pickupTime ASC")
    List<Ride> findByPickupTimeBetweenWithDriversAndPatient(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "WHERE r.pickupTime BETWEEN :start AND :end AND r.status = :status " +
            "ORDER BY r.pickupTime ASC")
    List<Ride> findByPickupTimeBetweenAndStatusWithDriversAndPatient(@Param("start") LocalDateTime start,
                                                                     @Param("end") LocalDateTime end,
                                                                     @Param("status") RideStatus status);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "WHERE (r.pickupDriver.id = :driverId OR r.dropoffDriver.id = :driverId) " +
            "AND r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.pickupTime ASC")
    List<Ride> findByAnyDriverAndPickupTimeBetweenWithPatient(@Param("driverId") Long driverId,
                                                              @Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "WHERE r.status = 'SCHEDULED' AND r.pickupDriver IS NULL AND r.dropoffDriver IS NULL " +
            "AND r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.priority DESC, r.pickupTime ASC")
    List<Ride> findUnassignedRidesInTimeRangeWithPatient(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    // Using @EntityGraph for specific scenarios
    @EntityGraph(attributePaths = {"patient", "pickupDriver", "dropoffDriver"})
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdWithFullDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"patient", "pickupDriver", "dropoffDriver", "auditHistory"})
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdWithAudit(@Param("id") Long id);

    // Optimized query for optimization service
    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "WHERE r.pickupLocation.latitude IS NOT NULL AND r.pickupLocation.longitude IS NOT NULL " +
            "AND r.dropoffLocation.latitude IS NOT NULL AND r.dropoffLocation.longitude IS NOT NULL " +
            "AND r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.priority DESC, r.pickupTime ASC")
    List<Ride> findRidesWithValidCoordinatesAndPatient(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    // Legacy method - keep for backward compatibility but use optimized version
    @Deprecated
    List<Ride> findByPickupTimeBetween(LocalDateTime start, LocalDateTime end);


        // Basic Time-based Queries

        List<Ride> findByPickupTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, RideStatus status);

        List<Ride> findByPickupTimeBetweenAndPriority(LocalDateTime start, LocalDateTime end, Priority priority);

        // Driver Assignment Queries
        List<Ride> findByPickupDriverIdAndPickupTimeBetween(Long driverId, LocalDateTime start, LocalDateTime end);

        List<Ride> findByDropoffDriverIdAndPickupTimeBetween(Long driverId, LocalDateTime start, LocalDateTime end);

        @Query("SELECT r FROM Ride r WHERE (r.pickupDriver.id = :driverId OR r.dropoffDriver.id = :driverId) " +
                "AND r.pickupTime BETWEEN :start AND :end")
        List<Ride> findByAnyDriverAndPickupTimeBetween(@Param("driverId") Long driverId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

        // Status and Priority Queries
        List<Ride> findByStatusOrderByPickupTimeAsc(RideStatus status);

        List<Ride> findByPriorityAndStatusOrderByPickupTimeAsc(Priority priority, RideStatus status);

        List<Ride> findByStatusInOrderByPickupTimeAsc(List<RideStatus> statuses);

        // Vehicle Type Queries
        List<Ride> findByRequiredVehicleTypeAndPickupTimeBetween(String vehicleType, LocalDateTime start, LocalDateTime end);

        @Query("SELECT r FROM Ride r WHERE r.requiredVehicleType IN :vehicleTypes AND r.pickupTime BETWEEN :start AND :end")
        List<Ride> findByVehicleTypesAndPickupTimeBetween(@Param("vehicleTypes") List<String> vehicleTypes,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);

        // Round Trip and Special Queries
        List<Ride> findByIsRoundTripTrueAndPickupTimeBetween(LocalDateTime start, LocalDateTime end);

        List<Ride> findByRideTypeAndPickupTimeBetween(RideType rideType, LocalDateTime start, LocalDateTime end);

        // Unassigned Rides
        @Query("SELECT r FROM Ride r WHERE r.status = 'SCHEDULED' AND r.pickupDriver IS NULL AND r.dropoffDriver IS NULL " +
                "AND r.pickupTime BETWEEN :start AND :end ORDER BY r.priority DESC, r.pickupTime ASC")
        List<Ride> findUnassignedRidesInTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // Optimization Batch Queries
        List<Ride> findByOptimizationBatchId(String batchId);

        @Query("SELECT r FROM Ride r WHERE r.optimizationBatchId = :batchId AND r.status = :status")
        List<Ride> findByOptimizationBatchIdAndStatus(@Param("batchId") String batchId, @Param("status") RideStatus status);

        // Patient-specific Queries
        List<Ride> findByPatientIdAndPickupTimeBetween(Long patientId, LocalDateTime start, LocalDateTime end);

        @Query("SELECT r FROM Ride r WHERE r.patient.id = :patientId ORDER BY r.pickupTime DESC")
        List<Ride> findByPatientIdOrderByPickupTimeDesc(@Param("patientId") Long patientId);

        // Statistics and Analytics
        @Query("SELECT COUNT(r) FROM Ride r WHERE r.status = :status AND r.pickupTime BETWEEN :start AND :end")
        long countByStatusAndPickupTimeBetween(@Param("status") RideStatus status,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

        @Query("SELECT r.priority, COUNT(r) FROM Ride r WHERE r.pickupTime BETWEEN :start AND :end GROUP BY r.priority")
        List<Object[]> countByPriorityAndDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT r.requiredVehicleType, COUNT(r) FROM Ride r WHERE r.pickupTime BETWEEN :start AND :end " +
                "GROUP BY r.requiredVehicleType")
        List<Object[]> countByVehicleTypeAndDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // Distance and Location Queries
        @Query("SELECT r FROM Ride r WHERE r.distance IS NOT NULL AND r.distance > :minDistance " +
                "AND r.pickupTime BETWEEN :start AND :end")
        List<Ride> findLongDistanceRides(@Param("minDistance") Double minDistance,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

        // Complex Filtering
        @Query("SELECT r FROM Ride r WHERE " +
                "(:status IS NULL OR r.status = :status) AND " +
                "(:priority IS NULL OR r.priority = :priority) AND " +
                "(:vehicleType IS NULL OR r.requiredVehicleType = :vehicleType) AND " +
                "(:driverId IS NULL OR r.pickupDriver.id = :driverId OR r.dropoffDriver.id = :driverId) AND " +
                "r.pickupTime BETWEEN :start AND :end " +
                "ORDER BY r.pickupTime ASC")
        List<Ride> findRidesWithCriteria(@Param("status") RideStatus status,
                                         @Param("priority") Priority priority,
                                         @Param("vehicleType") String vehicleType,
                                         @Param("driverId") Long driverId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

        // Add this to your RideRepository interface:
        @Query("SELECT r FROM Ride r WHERE " +
                "r.pickupLocation.latitude IS NOT NULL AND r.pickupLocation.longitude IS NOT NULL AND " +
                "r.dropoffLocation.latitude IS NOT NULL AND r.dropoffLocation.longitude IS NOT NULL AND " +
                "r.pickupTime BETWEEN :start AND :end")
        List<Ride> findRidesWithValidCoordinatesInTimeRange(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

        @Query("SELECT r FROM Ride r WHERE " +
                "r.pickupLocation.latitude BETWEEN :minLat AND :maxLat AND " +
                "r.pickupLocation.longitude BETWEEN :minLng AND :maxLng AND " +
                "r.pickupTime BETWEEN :start AND :end")
        List<Ride> findRidesInGeographicBounds(@Param("minLat") Double minLat,
                                               @Param("maxLat") Double maxLat,
                                               @Param("minLng") Double minLng,
                                               @Param("maxLng") Double maxLng,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
    }
