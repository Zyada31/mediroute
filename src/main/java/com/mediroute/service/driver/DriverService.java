
package com.mediroute.service.driver;

import com.mediroute.dto.DriverDTO;
import com.mediroute.dto.DriverStatistics;
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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Driver createOrUpdateDriver(DriverDTO dto, boolean update) {
        log.info("🚗 {} driver: {}", update ? "Updating" : "Creating", dto.getName());

        Optional<Driver> existingOpt = driverRepository.findByNameAndPhone(dto.getName(), dto.getPhone());

        if (existingOpt.isPresent() && !update) {
            throw new IllegalStateException("❌ Driver already exists with name & phone: "
                    + dto.getName() + ", " + dto.getPhone());
        }

        Driver driver = existingOpt.orElse(new Driver());

        updateDriverBasicInfo(driver, dto);
        updateDriverMedicalCapabilities(driver, dto);
        updateDriverLocation(driver, dto, update);
        updateDriverScheduling(driver, dto);

        driver = driverRepository.save(driver);
        log.info("✅ Driver {} successfully", update ? "updated" : "created");

        return driver;
    }

    @Transactional(readOnly = true)
    public List<Driver> getQualifiedDrivers() {
        return driverRepository.findByActiveTrue().stream()
                .filter(this::isDriverQualified)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Driver> getDriversByVehicleCapability(String vehicleType) {
        return driverRepository.findByActiveTrue().stream()
                .filter(driver -> canDriverHandleVehicleType(driver, vehicleType))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Driver> getAvailableDrivers(LocalDateTime startTime, LocalDateTime endTime) {
        return driverRepository.findByActiveTrue().stream()
                .filter(driver -> isDriverAvailable(driver, startTime, endTime))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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
                utilizationRate >= 100.0
        );
    }

    @Transactional(readOnly = true)
    public boolean canDriverHandlePatient(Driver driver, Patient patient) {
        if (driver == null || patient == null) return true;

        return !(Boolean.TRUE.equals(patient.getRequiresWheelchair()) &&
                !Boolean.TRUE.equals(driver.getWheelchairAccessible())) &&
                !(Boolean.TRUE.equals(patient.getRequiresStretcher()) &&
                        !Boolean.TRUE.equals(driver.getStretcherCapable())) &&
                !(Boolean.TRUE.equals(patient.getRequiresOxygen()) &&
                        !Boolean.TRUE.equals(driver.getOxygenEquipped()));
    }

    @Transactional(readOnly = true)
    public List<Driver> listAllDrivers() {
        return driverRepository.findAll();
    }

    // Private helper methods
    private void updateDriverBasicInfo(Driver driver, DriverDTO dto) {
        driver.setName(dto.getName());
        driver.setPhone(dto.getPhone());
        driver.setEmail(dto.getEmail());
        driver.setActive(true);
    }

    private void updateDriverMedicalCapabilities(Driver driver, DriverDTO dto) {
        if (dto.getVehicleType() != null) {
            try {
                VehicleTypeEnum vehicleType = VehicleTypeEnum.valueOf(dto.getVehicleType().toUpperCase());
                driver.setVehicleType(vehicleType);

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

        if (dto.getWheelchairAccessible() != null) {
            driver.setWheelchairAccessible(dto.getWheelchairAccessible());
        }
        if (dto.getStretcherCapable() != null) {
            driver.setStretcherCapable(dto.getStretcherCapable());
        }
        if (dto.getOxygenEquipped() != null) {
            driver.setOxygenEquipped(dto.getOxygenEquipped());
        }
        if (dto.getSkills() != null) {
            driver.setSkills(dto.getSkills());
        }
        if (dto.getCertifications() != null) {
            driver.setCertifications(dto.getCertifications());
        }

        Boolean trainingComplete = dto.getIsTrainingComplete() != null ?
                dto.getIsTrainingComplete() : dto.getTrainingComplete();
        if (trainingComplete != null) {
            driver.setIsTrainingComplete(trainingComplete);
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
        if (dto.getShiftStart() != null) {
            // Handle both LocalDateTime and LocalTime cases
            if (dto.getShiftStart() instanceof LocalDateTime) {
                driver.setShiftStart(((LocalDateTime) dto.getShiftStart()).toLocalTime());
            } else {
                // Assume it's already LocalTime or can be converted
                driver.setShiftStart(dto.getShiftStart().toLocalTime());
            }
        }
        if (dto.getShiftEnd() != null) {
            if (dto.getShiftEnd() instanceof LocalDateTime) {
                driver.setShiftEnd(((LocalDateTime) dto.getShiftEnd()).toLocalTime());
            } else {
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

    private boolean isDriverAvailable(Driver driver, LocalDateTime startTime, LocalDateTime endTime) {
        if (driver.getShiftStart() == null || driver.getShiftEnd() == null) {
            return true;
        }

        LocalTime requestStartTime = startTime.toLocalTime();
        LocalTime requestEndTime = endTime.toLocalTime();

        return !requestStartTime.isBefore(driver.getShiftStart()) &&
                !requestEndTime.isAfter(driver.getShiftEnd());
    }

    private boolean hasExpiredLicenses(Driver driver) {
        LocalDate today = LocalDate.now();

        return (driver.getDriversLicenseExpiry() != null && driver.getDriversLicenseExpiry().isBefore(today)) ||
                (driver.getMedicalTransportLicenseExpiry() != null && driver.getMedicalTransportLicenseExpiry().isBefore(today)) ||
                (driver.getInsuranceExpiry() != null && driver.getInsuranceExpiry().isBefore(today));
    }
    @Transactional(readOnly = true)
    public Optional<Driver> getDriverById(Long id) {
        return driverRepository.findById(id);
    }
    // Add these to DriverService:
    @Transactional(readOnly = true)
    public List<DriverWorkload> getAllDriverWorkloads(LocalDate date) {
        return driverRepository.findByActiveTrue().stream()
                .map(driver -> getDriverWorkload(driver.getId(), date))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverStatistics getDriverStats() {
        List<Driver> activeDrivers = driverRepository.findByActiveTrue();

        return DriverStatistics.builder()
                .activeDrivers((long) activeDrivers.size())
                .wheelchairCapableDrivers((int) activeDrivers.stream()
                        .filter(d -> Boolean.TRUE.equals(d.getWheelchairAccessible())).count())
                .stretcherCapableDrivers((int) activeDrivers.stream()
                        .filter(d -> Boolean.TRUE.equals(d.getStretcherCapable())).count())
                .oxygenEquippedDrivers((int) activeDrivers.stream()
                        .filter(d -> Boolean.TRUE.equals(d.getOxygenEquipped())).count())
                .build();
    }

    @Transactional
    public void deactivateDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        driver.setActive(false);
        driverRepository.save(driver);
    }

    @Transactional
    public void reactivateDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        if (!Boolean.TRUE.equals(driver.getIsTrainingComplete())) {
            throw new IllegalStateException("Cannot reactivate driver without completed training");
        }

        driver.setActive(true);
        driverRepository.save(driver);
    }

    // Supporting Classes
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
}
