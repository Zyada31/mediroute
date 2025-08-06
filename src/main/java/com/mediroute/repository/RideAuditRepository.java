package com.mediroute.repository;

import com.mediroute.entity.RideAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideAuditRepository extends JpaRepository<RideAudit, Long> {
    List<RideAudit> findByRideIdOrderByChangedAtDesc(Long rideId);

}
