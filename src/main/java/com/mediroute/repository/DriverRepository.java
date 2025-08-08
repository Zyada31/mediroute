package com.mediroute.repository;

import com.mediroute.dto.VehicleTypeEnum;
import com.mediroute.entity.Driver;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@Tag(name = "Driver Repository", description = "Driver data access operations")
public interface DriverRepository extends BaseRepository<Driver, Long> {

    // Basic Queries
    List<Driver> findByActiveTrue();

    List<Driver> findByActiveTrueAndIsTrainingCompleteTrue();

    List<Driver> findByNameContainingIgnoreCase(String name);

    List<Driver> findByPhone(String phone);

    // ADD THIS MISSING METHOD
    Optional<Driver> findByNameAndPhone(String name, String phone);

    // Vehicle Type Queries
    List<Driver> findByVehicleTypeAndActiveTrue(VehicleTypeEnum vehicleType);

    List<Driver> findByVehicleTypeInAndActiveTrue(List<VehicleTypeEnum> vehicleTypes);

    // Medical Transport Capabilities
    List<Driver> findByWheelchairAccessibleTrueAndActiveTrue();

    List<Driver> findByStretcherCapableTrueAndActiveTrue();

    List<Driver> findByOxygenEquippedTrueAndActiveTrue();

    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.isTrainingComplete = true AND " +
            "(d.wheelchairAccessible = true OR d.stretcherCapable = true OR d.oxygenEquipped = true)")
    List<Driver> findMedicalTransportQualifiedDrivers();

    // Availability Queries - SIMPLIFIED
    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.isTrainingComplete = true")
    List<Driver> findAvailableDriversAtTime(@Param("shiftTime") LocalTime shiftTime);

    // Location-based Queries
    @Query("SELECT d FROM Driver d WHERE d.active = true AND " +
            "6371 * acos(cos(radians(:lat)) * cos(radians(d.baseLat)) * " +
            "cos(radians(d.baseLng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(d.baseLat))) <= :radius")
    List<Driver> findDriversWithinRadius(@Param("lat") Double latitude,
                                         @Param("lng") Double longitude,
                                         @Param("radius") Double radiusKm);

    // License and Compliance
    @Query("SELECT d FROM Driver d WHERE d.active = true AND " +
            "(d.driversLicenseExpiry IS NULL OR d.driversLicenseExpiry > :checkDate) AND " +
            "(d.medicalTransportLicenseExpiry IS NULL OR d.medicalTransportLicenseExpiry > :checkDate) AND " +
            "(d.insuranceExpiry IS NULL OR d.insuranceExpiry > :checkDate)")
    List<Driver> findDriversWithValidLicenses(@Param("checkDate") LocalDate checkDate);

    @Query("SELECT d FROM Driver d WHERE d.active = true AND " +
            "(d.driversLicenseExpiry <= :expiryDate OR " +
            "d.medicalTransportLicenseExpiry <= :expiryDate OR " +
            "d.insuranceExpiry <= :expiryDate)")
    List<Driver> findDriversWithExpiringLicenses(@Param("expiryDate") LocalDate expiryDate);

    // REMOVED PROBLEMATIC SKILL-BASED QUERIES - These cause JPA validation errors with JSONB fields
    // Instead, we'll handle skill filtering in the service layer

    // Simplified Certified Drivers Query
    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.isTrainingComplete = true")
    List<Driver> findCertifiedDrivers();

    // Workload Queries - SIMPLIFIED
    @Query("SELECT d FROM Driver d WHERE d.active = true")
    List<Driver> findAvailableDriversForDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Statistics
    @Query("SELECT COUNT(d) FROM Driver d WHERE d.active = true")
    long countActiveDrivers();

    @Query("SELECT d.vehicleType, COUNT(d) FROM Driver d WHERE d.active = true GROUP BY d.vehicleType")
    List<Object[]> countDriversByVehicleType();

    // Complex Filtering - SIMPLIFIED
    @Query("SELECT d FROM Driver d WHERE " +
            "(:active IS NULL OR d.active = :active) AND " +
            "(:vehicleType IS NULL OR d.vehicleType = :vehicleType) AND " +
            "(:wheelchairAccessible IS NULL OR d.wheelchairAccessible = :wheelchairAccessible) AND " +
            "(:stretcherCapable IS NULL OR d.stretcherCapable = :stretcherCapable) AND " +
            "(:trainingComplete IS NULL OR d.isTrainingComplete = :trainingComplete)")
    List<Driver> findDriversWithCriteria(@Param("active") Boolean active,
                                         @Param("vehicleType") VehicleTypeEnum vehicleType,
                                         @Param("wheelchairAccessible") Boolean wheelchairAccessible,
                                         @Param("stretcherCapable") Boolean stretcherCapable,
                                         @Param("trainingComplete") Boolean trainingComplete);
}