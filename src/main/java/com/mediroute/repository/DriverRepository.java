// ============================================================================
// FIXED DRIVER REPOSITORY - REMOVING PROBLEMATIC QUERIES
// ============================================================================

package com.mediroute.repository;

import com.mediroute.dto.VehicleTypeEnum;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Patient;
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

    // ========== BASIC QUERIES (WORKING) ==========

    List<Driver> findByActiveTrue();

    List<Driver> findByActiveTrueAndIsTrainingCompleteTrue();

    List<Driver> findByNameContainingIgnoreCase(String name);

    List<Driver> findByPhone(String phone);

    Optional<Driver> findByNameAndPhone(String name, String phone);

    // ========== VEHICLE TYPE QUERIES (WORKING) ==========

    List<Driver> findByVehicleTypeAndActiveTrue(VehicleTypeEnum vehicleType);

    List<Driver> findByVehicleTypeInAndActiveTrue(List<VehicleTypeEnum> vehicleTypes);

    // ========== MEDICAL TRANSPORT CAPABILITIES (WORKING) ==========

    List<Driver> findByWheelchairAccessibleTrueAndActiveTrue();

    List<Driver> findByStretcherCapableTrueAndActiveTrue();

    List<Driver> findByOxygenEquippedTrueAndActiveTrue();

    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.isTrainingComplete = true AND " +
            "(d.wheelchairAccessible = true OR d.stretcherCapable = true OR d.oxygenEquipped = true)")
    List<Driver> findMedicalTransportQualifiedDrivers();

    // ========== AVAILABILITY QUERIES (WORKING) ==========

    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.isTrainingComplete = true")
    List<Driver> findAvailableDriversAtTime(@Param("shiftTime") LocalTime shiftTime);

    @Query("SELECT d FROM Driver d WHERE d.active = true")
    List<Driver> findAvailableDriversForDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ========== LOCATION-BASED QUERIES (WORKING) ==========

    @Query("SELECT d FROM Driver d WHERE d.active = true AND " +
            "6371 * acos(cos(radians(:lat)) * cos(radians(d.baseLat)) * " +
            "cos(radians(d.baseLng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(d.baseLat))) <= :radius")
    List<Driver> findDriversWithinRadius(@Param("lat") Double latitude,
                                         @Param("lng") Double longitude,
                                         @Param("radius") Double radiusKm);

    // ========== LICENSE AND COMPLIANCE (WORKING) ==========

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

    // ========== CERTIFICATION QUERIES (WORKING) ==========

    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.isTrainingComplete = true")
    List<Driver> findCertifiedDrivers();

    // ========== STATISTICS QUERIES (WORKING) ==========

    @Query("SELECT COUNT(d) FROM Driver d WHERE d.active = true")
    long countActiveDrivers();

    @Query("SELECT d.vehicleType, COUNT(d) FROM Driver d WHERE d.active = true GROUP BY d.vehicleType")
    List<Object[]> countDriversByVehicleType();

    // ========== COMPLEX FILTERING (WORKING) ==========

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
    @Query("SELECT p FROM Patient p LEFT JOIN FETCH p.rides WHERE p.id = :id")
    Optional<Patient> findByIdWithRides(@Param("id") Long id);
    // ========== REMOVED PROBLEMATIC QUERY ==========
    // The findDriverWithTodaysRides query caused issues because:
    // 1. Driver entity doesn't have proper pickupRides/dropoffRides mappings
    // 2. JPA validation fails on the relationship joins
    // 
    // SOLUTION: Handle driver workload in the service layer using RideRepository
    // See DriverService.getDriverWorkload() method for the correct implementation
}
