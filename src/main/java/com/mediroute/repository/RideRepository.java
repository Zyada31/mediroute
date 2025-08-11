// Fixed RideRepository with proper JOIN FETCH queries
package com.mediroute.repository;

import com.mediroute.dto.Priority;
import com.mediroute.dto.RideStatus;
import com.mediroute.entity.Ride;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends BaseRepository<Ride, Long> {

    // ========== FIXED QUERIES WITH PROPER JOIN FETCH ==========

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "LEFT JOIN FETCH r.driver d " +  // Legacy compatibility
            "WHERE r.id = :id")
    Optional<Ride> findByIdWithFullDetails(@Param("id") Long id);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "LEFT JOIN FETCH r.driver d " +
            "WHERE r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.pickupTime ASC")
    List<Ride> findByPickupTimeBetweenWithDriversAndPatient(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "LEFT JOIN FETCH r.driver d " +
            "WHERE r.pickupTime BETWEEN :start AND :end AND r.status = :status " +
            "ORDER BY r.pickupTime ASC")
    List<Ride> findByPickupTimeBetweenAndStatusWithDriversAndPatient(@Param("start") LocalDateTime start,
                                                                     @Param("end") LocalDateTime end,
                                                                     @Param("status") RideStatus status);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "LEFT JOIN FETCH r.driver d " +
            "WHERE (r.pickupDriver.id = :driverId OR r.dropoffDriver.id = :driverId " +
            "OR r.driver.id = :driverId) " +
            "AND r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.pickupTime ASC")
    List<Ride> findByAnyDriverAndPickupTimeBetweenWithPatient(@Param("driverId") Long driverId,
                                                              @Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "WHERE r.status = 'SCHEDULED' " +
            "AND r.pickupDriver IS NULL AND r.dropoffDriver IS NULL " +
            "AND r.driver IS NULL " +
            "AND r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.priority DESC, r.pickupTime ASC")
    List<Ride> findUnassignedRidesInTimeRangeWithPatient(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    // ========== OPTIMIZER-SPECIFIC QUERIES ==========

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "LEFT JOIN FETCH r.pickupDriver pd " +
            "LEFT JOIN FETCH r.dropoffDriver dd " +
            "WHERE r.pickupLocation.latitude IS NOT NULL " +
            "AND r.pickupLocation.longitude IS NOT NULL " +
            "AND r.dropoffLocation.latitude IS NOT NULL " +
            "AND r.dropoffLocation.longitude IS NOT NULL " +
            "AND r.pickupTime BETWEEN :start AND :end " +
            "ORDER BY r.priority DESC, r.pickupTime ASC")
    List<Ride> findRidesWithValidCoordinatesAndPatient(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.patient p " +
            "WHERE r.id IN :rideIds " +
            "ORDER BY r.priority DESC, r.pickupTime ASC")
    List<Ride> findByIdInWithPatient(@Param("rideIds") List<Long> rideIds);

    // ========== LEGACY COMPATIBILITY ==========

    @Query("SELECT r FROM Ride r WHERE r.pickupTime BETWEEN :start AND :end")
    List<Ride> findByPickupTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ========== DRIVER ASSIGNMENT QUERIES ==========

    List<Ride> findByPickupDriverIdAndPickupTimeBetween(Long driverId, LocalDateTime start, LocalDateTime end);
    List<Ride> findByDropoffDriverIdAndPickupTimeBetween(Long driverId, LocalDateTime start, LocalDateTime end);

    // ========== STATUS AND PRIORITY QUERIES ==========

    List<Ride> findByStatusOrderByPickupTimeAsc(RideStatus status);
    List<Ride> findByPriorityAndStatusOrderByPickupTimeAsc(Priority priority, RideStatus status);

    @Query("SELECT COUNT(r) FROM Ride r WHERE r.status = :status AND r.pickupTime BETWEEN :start AND :end")
    long countByStatusAndPickupTimeBetween(@Param("status") RideStatus status,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);
}