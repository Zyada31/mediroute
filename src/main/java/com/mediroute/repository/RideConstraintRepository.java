// 9. RideConstraint Repository
package com.mediroute.repository;

import com.mediroute.entity.RideConstraint;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Tag(name = "Ride Constraint Repository", description = "Ride constraint data access operations")
public interface RideConstraintRepository extends BaseRepository<RideConstraint, Long> {

    // Basic Queries
    Optional<RideConstraint> findByRideId(Long rideId);

    List<RideConstraint> findByMustBeSoloTrue();

    List<RideConstraint> findByRequiresAttendantTrue();

    // Driver Constraints
    List<RideConstraint> findByPreferredDriverId(Long driverId);

    List<RideConstraint> findByExcludedDriverId(Long driverId);

    @Query("SELECT rc FROM RideConstraint rc WHERE rc.preferredDriverId IS NOT NULL")
    List<RideConstraint> findConstraintsWithPreferredDriver();

    // Time Constraints
    @Query("SELECT rc FROM RideConstraint rc WHERE rc.fixedPickupTime = true")
    List<RideConstraint> findConstraintsWithFixedPickupTime();

    @Query("SELECT rc FROM RideConstraint rc WHERE rc.earliestPickup <= :currentTime AND rc.latestDropoff >= :currentTime")
    List<RideConstraint> findActiveConstraintsAtTime(@Param("currentTime") LocalDateTime currentTime);

    // Special Requirements
    List<RideConstraint> findByDirectRouteOnlyTrue();

    List<RideConstraint> findByTemperatureControlledTrue();

    @Query("SELECT rc FROM RideConstraint rc WHERE rc.maxDetourMinutes IS NOT NULL AND rc.maxDetourMinutes <= :maxMinutes")
    List<RideConstraint> findConstraintsWithMaxDetour(@Param("maxMinutes") Integer maxMinutes);
}
