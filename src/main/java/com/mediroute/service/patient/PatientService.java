package com.mediroute.service.patient;

import com.mediroute.dto.PatientDTO;
import com.mediroute.dto.PatientSummaryDTO;
import com.mediroute.dto.PatientUpdateDTO;
import com.mediroute.entity.Patient;
import com.mediroute.entity.PatientHistory;
import com.mediroute.exceptions.PatientNotFoundException;
import com.mediroute.repository.PatientRepository;
import com.mediroute.repository.PatientHistoryRepository;
import com.mediroute.service.base.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.v3.oas.annotations.tags.Tag;
import static com.mediroute.config.SecurityBeans.currentOrgId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Service", description = "Patient management operations")
public class PatientService implements BaseService<Patient, Long> {

    private final PatientRepository patientRepository;
    private final PatientHistoryRepository patientHistoryRepository;

    // ========== Base Service Implementation ==========

    @Override
    @Transactional
    public Patient save(Patient patient) {
        log.debug("Saving patient: {}", patient.getName());
        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findById(Long id) {
        return patientRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Patient> findAll(Specification<Patient> specification, Pageable pageable) {
        return patientRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("Soft deleting patient with ID: {}", id);
        Patient patient = findById(id).orElseThrow(() -> new PatientNotFoundException(id));
        patient.setIsActive(false);
        save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return patientRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return patientRepository.count();
    }

    // ========== Enhanced Patient Operations ==========

    @Transactional
    public Patient createPatient(PatientDTO createDTO) {
        log.info("Creating new patient: {}", createDTO.getName());

        // Check for duplicates within org
        Long org = currentOrgId();
        Optional<Patient> existingPatient = (org != null)
                ? patientRepository.findByPhoneAndOrgId(createDTO.getPhone(), org)
                : patientRepository.findByNameAndPhone(createDTO.getName(), createDTO.getPhone());

        if (existingPatient.isPresent()) {
            log.warn("Patient already exists with name: {} and phone: {}",
                    createDTO.getName(), createDTO.getPhone());
            return existingPatient.get();
        }

        Patient patient = Patient.builder()
                .name(createDTO.getName())
                .phone(createDTO.getPhone())
                .email(createDTO.getEmail())
                .emergencyContactName(createDTO.getEmergencyContactName())
                .emergencyContactPhone(createDTO.getEmergencyContactPhone())
                .requiresWheelchair(createDTO.getRequiresWheelchair())
                .requiresStretcher(createDTO.getRequiresStretcher())
                .requiresOxygen(createDTO.getRequiresOxygen())
                .mobilityLevel(createDTO.getMobilityLevel())
                .insuranceProvider(createDTO.getInsuranceProvider())
                .insuranceId(createDTO.getInsuranceId())
                .dateOfBirth(createDTO.getDateOfBirth())
                .isActive(true)
                .build();
        if (org != null) patient.setOrgId(org);

        return save(patient);
    }

    @Transactional
    public Patient updatePatient(Long id, PatientUpdateDTO updateDTO) {
        log.info("Updating patient with ID: {}", id);

        Patient patient = findById(id).orElseThrow(() -> new PatientNotFoundException(id));

        // Update fields
        if (updateDTO.getName() != null) patient.setName(updateDTO.getName());
        if (updateDTO.getPhone() != null) patient.setPhone(updateDTO.getPhone());
        if (updateDTO.getEmail() != null) patient.setEmail(updateDTO.getEmail());
        if (updateDTO.getEmergencyContactName() != null)
            patient.setEmergencyContactName(updateDTO.getEmergencyContactName());
        if (updateDTO.getEmergencyContactPhone() != null)
            patient.setEmergencyContactPhone(updateDTO.getEmergencyContactPhone());
        if (updateDTO.getRequiresWheelchair() != null)
            patient.setRequiresWheelchair(updateDTO.getRequiresWheelchair());
        if (updateDTO.getRequiresStretcher() != null)
            patient.setRequiresStretcher(updateDTO.getRequiresStretcher());
        if (updateDTO.getRequiresOxygen() != null)
            patient.setRequiresOxygen(updateDTO.getRequiresOxygen());
        if (updateDTO.getMobilityLevel() != null)
            patient.setMobilityLevel(updateDTO.getMobilityLevel());
        if (updateDTO.getInsuranceProvider() != null)
            patient.setInsuranceProvider(updateDTO.getInsuranceProvider());
        if (updateDTO.getInsuranceId() != null)
            patient.setInsuranceId(updateDTO.getInsuranceId());

        return save(patient);
    }

    // ========== Search and Filter Operations ==========

    @Transactional(readOnly = true)
    public List<Patient> findActivePatients() {
        return patientRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Patient> findByPhone(String phone) {
        return patientRepository.findByPhone(phone);
    }

    @Transactional(readOnly = true)
    public List<Patient> searchPatients(String searchTerm) {
        return patientRepository.findByNameContainingIgnoreCase(searchTerm);
    }

    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithSpecialNeeds() {
        return patientRepository.findPatientsWithSpecialNeeds();
    }

    @Transactional(readOnly = true)
    public List<Patient> findPatientsByInsurance(String insuranceProvider) {
        return patientRepository.findByInsuranceProvider(insuranceProvider);
    }

    @Transactional(readOnly = true)
    public List<Patient> findPatientsWithCriteria(String name, String phone, Boolean isActive) {
        return patientRepository.findPatientsWithCriteria(name, phone, isActive);
    }

    // ========== Medical Transport Specific Operations ==========

    @Transactional(readOnly = true)
    public List<Patient> findWheelchairPatients() {
        return patientRepository.findByRequiresWheelchairTrue();
    }

    @Transactional(readOnly = true)
    public List<Patient> findStretcherPatients() {
        return patientRepository.findByRequiresStretcherTrue();
    }

    @Transactional(readOnly = true)
    public List<Patient> findOxygenPatients() {
        return patientRepository.findByRequiresOxygenTrue();
    }

    // ========== Patient History Operations ==========

    @Transactional(readOnly = true)
    public List<PatientHistory> getPatientHistoryInDateRange(Long patientId, LocalDateTime start, LocalDateTime end) {
        return patientHistoryRepository.findByPatientAndDateRange(patientId, start, end);
    }

    @Transactional(readOnly = true)
    public PatientSummaryDTO getPatientSummary(Long patientId) {
        Patient patient = findById(patientId).orElseThrow(() -> new PatientNotFoundException(patientId));

        long totalRides = patientHistoryRepository.countCompletedRidesForPatient(patientId);
        long onTimeRides = patientHistoryRepository.countOnTimeRidesForPatient(patientId);
        Double avgDistance = patientHistoryRepository.getAverageDistanceForPatient(patientId);
        Double avgDuration = patientHistoryRepository.getAverageDurationForPatient(patientId);

        return PatientSummaryDTO.builder()
                .patientId(patientId)
                .name(patient.getName())
                .totalRides(totalRides)
                .onTimeRides(onTimeRides)
                .onTimePercentage(totalRides > 0 ? (onTimeRides * 100.0 / totalRides) : 0.0)
                .averageDistance(avgDistance)
                .averageDuration(avgDuration)
                .hasSpecialNeeds(patient.requiresSpecialVehicle())
                .build();
    }

    // ========== Statistics Operations ==========

    @Transactional(readOnly = true)
    public long getActivePatientCount() {
        return patientRepository.findByIsActiveTrue().size();
    }

    @Transactional(readOnly = true)
    public long getWheelchairPatientCount() {
        return patientRepository.countActiveWheelchairPatients();
    }

    @Transactional(readOnly = true)
    public long getStretcherPatientCount() {
        return patientRepository.countActiveStretcherPatients();
    }
}
