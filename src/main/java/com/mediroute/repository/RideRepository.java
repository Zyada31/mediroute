package com.mediroute.repository;

import com.mediroute.dto.Priority;
import com.mediroute.dto.RideStatus;
import com.mediroute.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByPickupTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Ride> findByStatus(String status);
    List<Ride> findByPickupTimeBetweenAndDriverIsNotNull(LocalDateTime start, LocalDateTime end);
    List<Ride> findByStatusAndPickupTimeBetween(String scheduled, LocalDateTime from, LocalDateTime to);

    List<Ride> findByPickupDriverIdAndPickupTimeBetween(Long driverId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Ride> findByDropoffDriverIdAndPickupTimeBetween(Long driverId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Ride> findByPickupDriverIdAndPickupTimeBetweenAndStatus(Long driverId, LocalDateTime start, LocalDateTime end, RideStatus rideStatus);

    List<Ride> findByPickupTimeBetweenAndStatus(LocalDateTime localDateTime, LocalDateTime localDateTime1, RideStatus rideStatus);

    List<Ride> findByPickupTimeBetweenAndRequiredVehicleType(LocalDateTime localDateTime, LocalDateTime localDateTime1, String vehicleType);

    List<Ride> findByPriorityAndStatusOrderByPickupTimeAsc(Priority priority, RideStatus rideStatus);
}