package com.mediroute.service.ride;

import com.mediroute.dto.*;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.entity.RideAudit;
import com.mediroute.exceptions.RideNotFoundException;
import com.mediroute.repository.RideRepository;
import com.mediroute.repository.RideAuditRepository;
import com.mediroute.service.base.BaseService;
import com.mediroute.service.parser.ExcelParserService;
import com.mediroute.utils.LocationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ride Service", description = "Ride management operations")
public class RideService implements BaseService<Ride, Long> {

    private final RideRepository rideRepository;
    private final RideAuditRepository rideAuditRepository;
    private final ExcelParserService excelParserService;
    private final OptimizationService optimizationService;

    // ========== Base Service Implementation ==========

    @Override
    @Transactional
    public Ride save(Ride ride) {
        log.debug("Saving ride for patient: {}", ride.getPatient().getName());
        return rideRepository.save(ride);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ride> findById(Long id) {
        return rideRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ride> findAll() {
        return rideRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ride> findAll(Pageable pageable) {
        return rideRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ride> findAll(Specification<Ride> specification, Pageable pageable) {
        return rideRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting ride with ID: {}", id);
        rideRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return rideRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return rideRepository.count();
    }

    @Transactional
    public Ride createRide(RideCreateDTO createDTO) {
        log.info("Creating new ride for patient ID: {}", createDTO.getPatientId());

        Ride ride = Ride.builder()
                .patient(Patient.builder().id(createDTO.getPatientId()).build())
                .pickupLocation(LocationUtils.createLocationFromAddress(createDTO.getPickupLocation()))
                .dropoffLocation(LocationUtils.createLocationFromAddress(createDTO.getDropoffLocation()))
                .pickupTime(createDTO.getPickupTime())
                .dropoffTime(createDTO.getDropoffTime())
                .appointmentDuration(createDTO.getAppointmentDuration())
                .rideType(createDTO.getRideType())
                .priority(createDTO.getPriority())
                .requiredVehicleType(createDTO.getRequiredVehicleType())
                .requiredSkills(createDTO.getRequiredSkills())
                .isRoundTrip(createDTO.getIsRoundTrip())
                .status(RideStatus.SCHEDULED)
                .build();

        // Set time windows
        if (createDTO.getPickupTime() != null) {
            ride.setPickupTimeWindow(createDTO.getPickupTime(), 5);
        }
        if (createDTO.getDropoffTime() != null) {
            ride.setDropoffTimeWindow(createDTO.getDropoffTime(), 5);
        }

        return save(ride);
    }

    @Transactional
    public Ride updateRide(Long id, RideUpdateDTO updateDTO) {
        log.info("Updating ride with ID: {}", id);

        Ride ride = findById(id).orElseThrow(() -> new RideNotFoundException(id));
        Ride originalRide = createRideCopy(ride); // For audit trail

        // Update fields and create audit entries
        if (updateDTO.getPickupTime() != null && !updateDTO.getPickupTime().equals(ride.getPickupTime())) {
            createAuditEntry(ride, "pickupTime",
                    ride.getPickupTime().toString(), updateDTO.getPickupTime().toString(), "SYSTEM");
            ride.setPickupTime(updateDTO.getPickupTime());
            ride.setPickupTimeWindow(updateDTO.getPickupTime(), 5);
        }

        if (updateDTO.getDropoffTime() != null && !updateDTO.getDropoffTime().equals(ride.getDropoffTime())) {
            createAuditEntry(ride, "dropoffTime",
                    ride.getDropoffTime() != null ? ride.getDropoffTime().toString() : null,
                    updateDTO.getDropoffTime().toString(), "SYSTEM");
            ride.setDropoffTime(updateDTO.getDropoffTime());
            ride.setDropoffTimeWindow(updateDTO.getDropoffTime(), 5);
        }

        if (updateDTO.getStatus() != null && !updateDTO.getStatus().equals(ride.getStatus())) {
            createAuditEntry(ride, "status",
                    ride.getStatus().toString(), updateDTO.getStatus().toString(), "SYSTEM");
            ride.setStatus(updateDTO.getStatus());
        }

        if (updateDTO.getPriority() != null && !updateDTO.getPriority().equals(ride.getPriority())) {
            createAuditEntry(ride, "priority",
                    ride.getPriority().toString(), updateDTO.getPriority().toString(), "SYSTEM");
            ride.setPriority(updateDTO.getPriority());
        }

        return save(ride);
    }

    // ========== Excel Import Operations ==========

    @Transactional
    public ParseResult parseExcelFile(MultipartFile file, LocalDate assignmentDate) throws IOException {
        log.info("Parsing Excel file for assignment date: {}", assignmentDate);
        return excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, false);
    }

    @Transactional
    public ParseResult parseAndOptimizeExcelFile(MultipartFile file, LocalDate assignmentDate) throws IOException {
        log.info("Parsing and optimizing Excel file for assignment date: {}", assignmentDate);
        return excelParserService.parseExcelWithMedicalFeatures(file, assignmentDate, true);
    }

    // ========== Search and Filter Operations ==========

    @Transactional(readOnly = true)
    public List<Ride> findRidesByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByPickupTimeBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Ride> findRidesByDateAndStatus(LocalDate date, RideStatus status) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByPickupTimeBetweenAndStatus(start, end, status);
    }

    @Transactional(readOnly = true)
    public List<Ride> findRidesByDriver(Long driverId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByAnyDriverAndPickupTimeBetween(driverId, start, end);
    }

    @Transactional(readOnly = true)
    public List<Ride> findUnassignedRides(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findUnassignedRidesInTimeRange(start, end);
    }

    @Transactional(readOnly = true)
    public List<Ride> findEmergencyRides() {
        return rideRepository.findByPriorityAndStatusOrderByPickupTimeAsc(Priority.EMERGENCY, RideStatus.SCHEDULED);
    }

    @Transactional(readOnly = true)
    public List<Ride> findRidesByVehicleType(String vehicleType, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return rideRepository.findByRequiredVehicleTypeAndPickupTimeBetween(vehicleType, start, end);
    }

    @Transactional(readOnly = true)
    public List<Ride> findRidesByCriteria(RideSearchDTO criteria) {
        LocalDateTime start = criteria.getStartDate().atStartOfDay();
        LocalDateTime end = criteria.getEndDate().plusDays(1).atStartOfDay();

        return rideRepository.findRidesWithCriteria(
                criteria.getStatus(),
                criteria.getPriority(),
                criteria.getVehicleType(),
                criteria.getDriverId(),
                start,
                end
        );
    }

    // ========== Optimization Operations ==========

    @Transactional
    public OptimizationResult optimizeRides(List<Ride> rides) {
        log.info("Starting optimization for {} rides", rides.size());
        return optimizationService.optimizeSchedule(rides);
    }

    @Transactional
    public OptimizationResult optimizeRidesForDate(LocalDate date) {
        List<Ride> unassignedRides = findUnassignedRides(date);
        if (unassignedRides.isEmpty()) {
            log.info("No unassigned rides found for date: {}", date);
            return OptimizationResult.empty();
        }
        return optimizeRides(unassignedRides);
    }

    @Transactional
    public OptimizationResult reOptimizeAffectedRides(List<Long> affectedRideIds) {
        if (affectedRideIds == null || affectedRideIds.isEmpty()) {
            return OptimizationResult.empty();
        }

        List<Ride> affectedRides = rideRepository.findAllById(affectedRideIds);
        List<LocalDate> affectedDates = affectedRides.stream()
                .map(ride -> ride.getPickupTime().toLocalDate())
                .distinct()
                .toList();

        List<Ride> ridesToReOptimize = affectedDates.stream()
                .flatMap(date -> findUnassignedRides(date).stream())
                .toList();

        log.info("Re-optimizing {} rides affected by changes to {} rides on {} dates",
                ridesToReOptimize.size(), affectedRideIds.size(), affectedDates.size());

        return optimizeRides(ridesToReOptimize);
    }

    // ========== Audit Operations ==========

    @Transactional(readOnly = true)
    public List<RideAudit> getRideAuditHistory(Long rideId) {
        return rideAuditRepository.findByRideIdOrderByChangedAtDesc(rideId);
    }

    @Transactional
    private void createAuditEntry(Ride ride, String fieldName, String oldValue, String newValue, String changedBy) {
        RideAudit audit = RideAudit.builder()
                .ride(ride)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .systemGenerated(true)
                .build();

        rideAuditRepository.save(audit);
    }

    // ========== Statistics Operations ==========

    @Transactional(readOnly = true)
    public RideStatisticsDTO getRideStatistics(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Ride> allRides = rideRepository.findByPickupTimeBetween(start, end);

        long totalRides = allRides.size();
        long assignedRides = allRides.stream()
                .filter(Ride::isAssigned)
                .count();
        long emergencyRides = allRides.stream()
                .filter(ride -> ride.getPriority() == Priority.EMERGENCY)
                .count();
        long wheelchairRides = allRides.stream()
                .filter(ride -> "wheelchair_van".equals(ride.getRequiredVehicleType()))
                .count();
        long roundTripRides = allRides.stream()
                .filter(ride -> Boolean.TRUE.equals(ride.getIsRoundTrip()))
                .count();

        return RideStatisticsDTO.builder()
                .date(date)
                .totalRides((int) totalRides)
                .assignedRides((int) assignedRides)
                .unassignedRides((int) (totalRides - assignedRides))
                .emergencyRides((int) emergencyRides)
                .wheelchairRides((int) wheelchairRides)
                .roundTripRides((int) roundTripRides)
                .assignmentRate(totalRides > 0 ? (assignedRides * 100.0 / totalRides) : 0.0)
                .build();
    }

    // ========== Helper Methods ==========

    private Ride createRideCopy(Ride original) {
        return Ride.builder()
                .id(original.getId())
                .patient(original.getPatient())
                .pickupLocation(original.getPickupLocation())
                .dropoffLocation(original.getDropoffLocation())
                .pickupTime(original.getPickupTime())
                .dropoffTime(original.getDropoffTime())
                .status(original.getStatus())
                .priority(original.getPriority())
                .build();
    }
}