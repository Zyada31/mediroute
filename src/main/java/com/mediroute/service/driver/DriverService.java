package com.mediroute.service.driver;

import com.mediroute.dto.DriverDTO;
import com.mediroute.dto.VehicleTypeEnum;
import com.mediroute.entity.Driver;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.repository.DriverRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.distance.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final GeocodingService geocodingService;

    /**
     * Create or update a driver with enhanced medical transport capabilities
     */
    public Driver createOrUpdateDriver(DriverDTO dto, boolean update) {
        log.info("🚗 {} driver: {}", update ? "Updating" : "Creating", dto.getName());

        // Look up by Name + Phone for uniqueness
        Optional<Driver> existingOpt = driverRepository.findByNameAndPhone(dto.getName(), dto.getPhone());

        if (existingOpt.isPresent() && !update) {
            throw new IllegalStateException("❌ Driver already exists with name & phone: "
                    + dto.getName() + ", " + dto.getPhone());
        }

        Driver driver = existingOpt.orElse(new Driver());

        // Set basic driver information
        updateDriverBasicInfo(driver, dto);

        // Set medical transport capabilities
        updateDriverMedicalCapabilities(driver, dto);

        // Geocode and set base location
        updateDriverLocation(driver, dto, update);

        // Set scheduling information - FIXED TIME HANDLING
        updateDriverScheduling(driver, dto);

        driver = driverRepository.save(driver);
        log.info("✅ Driver {} successfully", update ? "updated" : "created");

        return driver;
    }

    /**
     * Get all qualified drivers for medical transport
     */
    public List<Driver> getQualifiedDrivers() {
        return driverRepository.findByActiveTrue().stream()
                .filter(this::isDriverQualified)
                .collect(Collectors.toList());
    }

    /**
     * Get drivers by vehicle type capability
     */
    public List<Driver> getDriversByVehicleCapability(String vehicleType) {
        List<Driver> allDrivers = driverRepository.findByActiveTrue();

        return allDrivers.stream()
                .filter(driver -> canDriverHandleVehicleType(driver, vehicleType))
                .collect(Collectors.toList());
    }

    /**
     * Get drivers available for a specific time period - FIXED
     */
    public List<Driver> getAvailableDrivers(LocalDateTime startTime, LocalDateTime endTime) {
        List<Driver> activeDrivers = driverRepository.findByActiveTrue();

        return activeDrivers.stream()
                .filter(driver -> isDriverAvailable(driver, startTime, endTime))
                .collect(Collectors.toList());
    }

    /**
     * Get driver workload for a specific date
     */
    public DriverWorkload getDriverWorkload(Long driverId, LocalDate date) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Ride> pickupRides = rideRepository.findByPickupDriverIdAndPickupTimeBetween(
                driverId, startOfDay, endOfDay);

        List<Ride> dropoffRides = rideRepository.findByDropoffDriverIdAndPickupTimeBetween(
                driverId, startOfDay, endOfDay);

        int totalRides = pickupRides.size() + dropoffRides.size();
        int maxDailyRides = driver.getMaxDailyRides() != null ? driver.getMaxDailyRides() : 8;

        double utilizationRate = maxDailyRides > 0 ? (totalRides * 100.0) / maxDailyRides : 0.0;

        return new DriverWorkload(
                driver.getId(),
                driver.getName(),
                date,
                pickupRides.size(),
                dropoffRides.size(),
                totalRides,
                maxDailyRides,
                utilizationRate,
                utilizationRate >= 100.0 // isOverloaded
        );
    }

    /**
     * Get all drivers with their current workload
     */
    public List<DriverWorkload> getAllDriverWorkloads(LocalDate date) {
        List<Driver> drivers = driverRepository.findByActiveTrue();

        return drivers.stream()
                .map(driver -> getDriverWorkload(driver.getId(), date))
                .sorted(Comparator.comparing(DriverWorkload::getUtilizationRate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Check if a driver can handle a specific patient's needs
     */
    public boolean canDriverHandlePatient(Driver driver, Patient patient) {
        if (driver == null || patient == null) return true;

        // Check wheelchair accessibility
        if (Boolean.TRUE.equals(patient.getRequiresWheelchair()) &&
                !Boolean.TRUE.equals(driver.getWheelchairAccessible())) {
            return false;
        }

        // Check stretcher capability
        if (Boolean.TRUE.equals(patient.getRequiresStretcher()) &&
                !Boolean.TRUE.equals(driver.getStretcherCapable())) {
            return false;
        }

        // Check oxygen equipment
        if (Boolean.TRUE.equals(patient.getRequiresOxygen()) &&
                !Boolean.TRUE.equals(driver.getOxygenEquipped())) {
            return false;
        }

        return true;
    }

    // ============================
    // Private Helper Methods
    // ============================

    private void updateDriverBasicInfo(Driver driver, DriverDTO dto) {
        driver.setName(dto.getName());
        driver.setPhone(dto.getPhone());
        driver.setEmail(dto.getEmail());
        driver.setActive(true);
    }

    private void updateDriverMedicalCapabilities(Driver driver, DriverDTO dto) {
        // Set vehicle type
        if (dto.getVehicleType() != null) {
            try {
                VehicleTypeEnum vehicleType = VehicleTypeEnum.valueOf(dto.getVehicleType().toUpperCase());
                driver.setVehicleType(vehicleType);

                // Auto-set capabilities based on vehicle type
                switch (vehicleType) {
                    case WHEELCHAIR_VAN:
                        driver.setWheelchairAccessible(true);
                        driver.setVehicleCapacity(4);
                        break;
                    case STRETCHER_VAN:
                        driver.setStretcherCapable(true);
                        driver.setWheelchairAccessible(true);
                        driver.setVehicleCapacity(2);
                        break;
                    case AMBULANCE:
                        driver.setStretcherCapable(true);
                        driver.setWheelchairAccessible(true);
                        driver.setOxygenEquipped(true);
                        driver.setVehicleCapacity(1);
                        break;
                    case VAN:
                        driver.setVehicleCapacity(6);
                        break;
                    case SEDAN:
                    default:
                        driver.setVehicleCapacity(4);
                        break;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid vehicle type: {}, defaulting to SEDAN", dto.getVehicleType());
                driver.setVehicleType(VehicleTypeEnum.SEDAN);
                driver.setVehicleCapacity(4);
            }
        }

        // Override with explicit capabilities if provided
        if (dto.getWheelchairAccessible() != null) {
            driver.setWheelchairAccessible(dto.getWheelchairAccessible());
        }
        if (dto.getStretcherCapable() != null) {
            driver.setStretcherCapable(dto.getStretcherCapable());
        }
        if (dto.getOxygenEquipped() != null) {
            driver.setOxygenEquipped(dto.getOxygenEquipped());
        }

        // Set skills
        if (dto.getSkills() != null) {
            driver.setSkills(dto.getSkills());
        }

        // Set certifications
        if (dto.getCertifications() != null) {
            driver.setCertifications(dto.getCertifications());
        }

        // Set training status
        if (dto.getIsTrainingComplete() != null) {
            driver.setIsTrainingComplete(dto.getIsTrainingComplete());
        } else if (dto.getTrainingComplete() != null) {
            driver.setIsTrainingComplete(dto.getTrainingComplete());
        }
    }

    private void updateDriverLocation(Driver driver, DriverDTO dto, boolean update) {
        if (dto.getBaseLocation() != null && !dto.getBaseLocation().isBlank()) {
            GeocodingService.GeoPoint geo = geocodingService.geocode(dto.getBaseLocation());
            if (geo == null) {
                throw new IllegalArgumentException("❌ Failed to geocode location: " + dto.getBaseLocation());
            }

            log.info("📍 Geocoded {} → lat={}, lng={}", dto.getBaseLocation(), geo.lat(), geo.lng());

            driver.setBaseLocation(dto.getBaseLocation());
            driver.setBaseLat(geo.lat());
            driver.setBaseLng(geo.lng());
        } else if (!update) {
            throw new IllegalArgumentException("❌ Base location is required for new drivers");
        }
    }

    // FIXED: Correct time handling
    private void updateDriverScheduling(Driver driver, DriverDTO dto) {
        // Handle LocalDateTime from DTO and convert to LocalTime
        if (dto.getShiftStart() != null) {
            if (dto.getShiftStart().toLocalTime() != null) {
                driver.setShiftStart(dto.getShiftStart().toLocalTime());
            }
        }
        if (dto.getShiftEnd() != null) {
            if (dto.getShiftEnd().toLocalTime() != null) {
                driver.setShiftEnd(dto.getShiftEnd().toLocalTime());
            }
        }
        if (dto.getMaxDailyRides() != null) {
            driver.setMaxDailyRides(dto.getMaxDailyRides());
        }
    }

    private boolean isDriverQualified(Driver driver) {
        return driver.getActive() &&
                Boolean.TRUE.equals(driver.getIsTrainingComplete()) &&
                !hasExpiredLicenses(driver);
    }

    private boolean canDriverHandleVehicleType(Driver driver, String vehicleType) {
        if (vehicleType == null || vehicleType.isEmpty()) return true;

        return switch (vehicleType.toLowerCase()) {
            case "wheelchair_van" -> Boolean.TRUE.equals(driver.getWheelchairAccessible());
            case "stretcher_van" -> Boolean.TRUE.equals(driver.getStretcherCapable());
            case "ambulance" -> Boolean.TRUE.equals(driver.getStretcherCapable()) &&
                    Boolean.TRUE.equals(driver.getOxygenEquipped());
            case "van" -> driver.getVehicleType().name().contains("VAN");
            case "sedan" -> true;
            default -> true;
        };
    }

    // FIXED: Correct time comparison
    private boolean isDriverAvailable(Driver driver, LocalDateTime startTime, LocalDateTime endTime) {
        if (driver.getShiftStart() == null || driver.getShiftEnd() == null) {
            return true; // No shift restrictions
        }

        // Extract time components for comparison
        LocalTime requestStartTime = startTime.toLocalTime();
        LocalTime requestEndTime = endTime.toLocalTime();

        // Check if the time period overlaps with driver's shift
        return !requestStartTime.isBefore(driver.getShiftStart()) &&
                !requestEndTime.isAfter(driver.getShiftEnd());
    }

    private boolean hasRequiredSkills(Driver driver, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) return true;
        if (driver.getSkills() == null) return false;

        return requiredSkills.stream()
                .allMatch(skill -> Boolean.TRUE.equals(driver.getSkills().get(skill)));
    }

    private boolean hasExpiredLicenses(Driver driver) {
        LocalDate today = LocalDate.now();

        return (driver.getDriversLicenseExpiry() != null && driver.getDriversLicenseExpiry().isBefore(today)) ||
                (driver.getMedicalTransportLicenseExpiry() != null && driver.getMedicalTransportLicenseExpiry().isBefore(today)) ||
                (driver.getInsuranceExpiry() != null && driver.getInsuranceExpiry().isBefore(today));
    }

    private boolean needsLicenseRenewal(Driver driver, LocalDate checkDate) {
        return (driver.getDriversLicenseExpiry() != null && driver.getDriversLicenseExpiry().isBefore(checkDate)) ||
                (driver.getMedicalTransportLicenseExpiry() != null && driver.getMedicalTransportLicenseExpiry().isBefore(checkDate)) ||
                (driver.getInsuranceExpiry() != null && driver.getInsuranceExpiry().isBefore(checkDate));
    }

    private double calculateDistanceToPickup(Driver driver, Ride ride) {
        // Simplified Haversine distance calculation
        if (ride.getPickupLocation() == null || !ride.getPickupLocation().isValid()) {
            return Double.MAX_VALUE;
        }

        double lat1 = Math.toRadians(driver.getBaseLat());
        double lon1 = Math.toRadians(driver.getBaseLng());
        double lat2 = Math.toRadians(ride.getPickupLocation().getLatitude());
        double lon2 = Math.toRadians(ride.getPickupLocation().getLongitude());

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return 6371 * c; // Earth's radius in kilometers
    }

    // ============================
    // Supporting Classes
    // ============================

    public static class DriverWorkload {
        private final Long driverId;
        private final String driverName;
        private final LocalDate date;
        private final int pickupRides;
        private final int dropoffRides;
        private final int totalRides;
        private final int maxDailyRides;
        private final double utilizationRate;
        private final boolean isOverloaded;

        public DriverWorkload(Long driverId, String driverName, LocalDate date,
                              int pickupRides, int dropoffRides, int totalRides,
                              int maxDailyRides, double utilizationRate, boolean isOverloaded) {
            this.driverId = driverId;
            this.driverName = driverName;
            this.date = date;
            this.pickupRides = pickupRides;
            this.dropoffRides = dropoffRides;
            this.totalRides = totalRides;
            this.maxDailyRides = maxDailyRides;
            this.utilizationRate = utilizationRate;
            this.isOverloaded = isOverloaded;
        }

        // Getters
        public Long getDriverId() { return driverId; }
        public String getDriverName() { return driverName; }
        public LocalDate getDate() { return date; }
        public int getPickupRides() { return pickupRides; }
        public int getDropoffRides() { return dropoffRides; }
        public int getTotalRides() { return totalRides; }
        public int getMaxDailyRides() { return maxDailyRides; }
        public double getUtilizationRate() { return utilizationRate; }
        public boolean isOverloaded() { return isOverloaded; }

        public int getAvailableCapacity() {
            return Math.max(0, maxDailyRides - totalRides);
        }
    }

    public static class DriverStats {
        private final int totalDrivers;
        private final int activeDrivers;
        private final int wheelchairCapableDrivers;
        private final int stretcherCapableDrivers;
        private final int oxygenEquippedDrivers;
        private final int trainedDrivers;
        private final int driversNeedingRenewal;

        public DriverStats(int totalDrivers, int activeDrivers, int wheelchairCapableDrivers,
                           int stretcherCapableDrivers, int oxygenEquippedDrivers,
                           int trainedDrivers, int driversNeedingRenewal) {
            this.totalDrivers = totalDrivers;
            this.activeDrivers = activeDrivers;
            this.wheelchairCapableDrivers = wheelchairCapableDrivers;
            this.stretcherCapableDrivers = stretcherCapableDrivers;
            this.oxygenEquippedDrivers = oxygenEquippedDrivers;
            this.trainedDrivers = trainedDrivers;
            this.driversNeedingRenewal = driversNeedingRenewal;
        }

        // Getters
        public int getTotalDrivers() { return totalDrivers; }
        public int getActiveDrivers() { return activeDrivers; }
        public int getWheelchairCapableDrivers() { return wheelchairCapableDrivers; }
        public int getStretcherCapableDrivers() { return stretcherCapableDrivers; }
        public int getOxygenEquippedDrivers() { return oxygenEquippedDrivers; }
        public int getTrainedDrivers() { return trainedDrivers; }
        public int getDriversNeedingRenewal() { return driversNeedingRenewal; }
    }

    /**
     * Get all drivers (for backward compatibility)
     */
    public List<Driver> listAllDrivers() {
        return driverRepository.findAll();
    }

    // Add these methods to your DriverService class to handle skill filtering:

    /**
     * Find drivers with specific skill - handled in service layer due to JSONB complexity
     */
    public List<Driver> findDriversWithSkill(String skillName) {
        return driverRepository.findByActiveTrue().stream()
                .filter(driver -> hasSkill(driver, skillName))
                .collect(Collectors.toList());
    }

    /**
     * Get drivers needing license renewal soon
     */
    public List<Driver> getDriversNeedingRenewal(int daysAhead) {
        LocalDate checkDate = LocalDate.now().plusDays(daysAhead);
        return driverRepository.findDriversWithExpiringLicenses(checkDate);
    }

    /**
     * Get driver statistics for reporting
     */
    public DriverStats getDriverStats() {
        List<Driver> allDrivers = driverRepository.findAll();
        List<Driver> activeDrivers = driverRepository.findByActiveTrue();

        long wheelchairCapableDrivers = activeDrivers.stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getWheelchairAccessible()))
                .count();

        long stretcherCapableDrivers = activeDrivers.stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getStretcherCapable()))
                .count();

        long oxygenEquippedDrivers = activeDrivers.stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getOxygenEquipped()))
                .count();

        long trainedDrivers = activeDrivers.stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getIsTrainingComplete()))
                .count();

        long driversNeedingRenewal = getDriversNeedingRenewal(30).size();

        return new DriverStats(
                allDrivers.size(),
                activeDrivers.size(),
                (int) wheelchairCapableDrivers,
                (int) stretcherCapableDrivers,
                (int) oxygenEquippedDrivers,
                (int) trainedDrivers,
                (int) driversNeedingRenewal
        );
    }

    /**
     * Find the best driver for an emergency ride
     */
    public Driver findBestEmergencyDriver(Ride emergencyRide, List<Driver> availableDrivers) {
        if (emergencyRide == null || availableDrivers == null || availableDrivers.isEmpty()) {
            return null;
        }

        return availableDrivers.stream()
                .filter(driver -> canDriverHandlePatient(driver, emergencyRide.getPatient()))
                .filter(driver -> hasRequiredSkills(driver, emergencyRide.getRequiredSkills()))
                .filter(driver -> !hasExpiredLicenses(driver))
                .min(Comparator.comparingDouble(driver -> calculateDistanceToPickup(driver, emergencyRide)))
                .orElse(null);
    }

    /**
     * Check if driver has a specific skill
     */
    private boolean hasSkill(Driver driver, String skillName) {
        if (driver.getSkills() == null || skillName == null) {
            return false;
        }
        return Boolean.TRUE.equals(driver.getSkills().get(skillName));
    }

    /**
     * Get drivers by shift time - simplified version
     */
    public List<Driver> getDriversByShiftTime(LocalDateTime time) {
        return driverRepository.findByActiveTrue().stream()
                .filter(driver -> isDriverAvailable(driver, time, time.plusHours(1)))
                .collect(Collectors.toList());
    }

    /**
     * Get emergency-qualified drivers
     */
    public List<Driver> getEmergencyQualifiedDrivers() {
        return driverRepository.findByActiveTrue().stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getIsTrainingComplete()))
                .filter(driver -> !hasExpiredLicenses(driver))
                .filter(driver -> hasEmergencySkills(driver))
                .collect(Collectors.toList());
    }

    private boolean hasEmergencySkills(Driver driver) {
        if (driver.getSkills() == null) return false;

        // Check for emergency-related skills
        return Boolean.TRUE.equals(driver.getSkills().get("CPR")) ||
                Boolean.TRUE.equals(driver.getSkills().get("First Aid")) ||
                Boolean.TRUE.equals(driver.getSkills().get("Emergency Response"));
    }

    /**
     * Deactivate a driver
     */
    public void deactivateDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        driver.setActive(false);
        driverRepository.save(driver);

        log.info("🚫 Driver {} has been deactivated", driver.getName());
    }

    /**
     * Reactivate a driver
     */
    public void reactivateDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        if (hasExpiredLicenses(driver)) {
            throw new IllegalStateException("Cannot reactivate driver with expired licenses: " + driver.getName());
        }

        driver.setActive(true);
        driverRepository.save(driver);

        log.info("✅ Driver {} has been reactivated", driver.getName());
    }

    /**
     * Update driver certifications
     */
    public void updateDriverCertifications(Long driverId, List<String> certifications) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        driver.setCertifications(certifications);
        driverRepository.save(driver);

        log.info("📜 Updated certifications for driver {}: {}", driver.getName(), certifications);
    }

    /**
     * Update driver skills
     */
    public void updateDriverSkills(Long driverId, Map<String, Boolean> skills) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        driver.setSkills(skills);
        driverRepository.save(driver);

        log.info("🎯 Updated skills for driver {}", driver.getName());
    }

    /**
     * Get driver by ID
     */
    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }
}