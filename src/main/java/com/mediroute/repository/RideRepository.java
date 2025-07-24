package com.mediroute.repository;

import com.mediroute.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByPickupTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Ride> findByStatus(String status);
    List<Ride> findByPickupTimeBetweenAndDriverIsNotNull(LocalDateTime start, LocalDateTime end);
    List<Ride> findByStatusAndPickupTimeBetween(String scheduled, LocalDateTime from, LocalDateTime to);
}