//package com.mediroute.service.parser;
//
//import com.mediroute.dto.*;
//import com.mediroute.entity.Patient;
//import com.mediroute.entity.Ride;
//import com.mediroute.repository.PatientRepository;
//import com.mediroute.repository.RideRepository;
//import com.mediroute.service.distance.GeocodingService;
//import com.mediroute.service.ride.EnhancedMedicalTransportOptimizer;
//import lombok.RequiredArgsConstructor;
//import org.apache.poi.ss.usermodel.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.time.*;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Component
//@RequiredArgsConstructor
//public class EnhancedExcelRideParser {
//    Logger log = LoggerFactory.getLogger(EnhancedExcelRideParser.class);
//
//    private final PatientRepository patientRepository;
//    private final RideRepository rideRepository;
//    private final GeocodingService geocodingService;
//    private final EnhancedMedicalTransportOptimizer rideOptimizerService;
//
//    // Keep your existing method for backward compatibility
//    public List<Ride> parseExcel(MultipartFile file, LocalDate assignmentDate) throws IOException {
//        return parseExcelWithMedicalFeatures(file, assignmentDate, false).getRides();
//    }
//
//    // New enhanced method with medical transport features
//    public ParseResult parseExcelWithMedicalFeatures(MultipartFile file, LocalDate assignmentDate, boolean runOptimization) throws IOException {
//        if (assignmentDate == null) assignmentDate = LocalDate.now().plusDays(1);
//
//        Workbook workbook = WorkbookFactory.create(file.getInputStream());
//        Sheet sheet = workbook.getSheetAt(0);
//
//        // Build header mapping with medical field recognition
//        Map<String, Integer> headerMap = buildEnhancedHeaderMapping(sheet);
//
//        List<Ride> rides = new ArrayList<>();
//        int skipped = 0;
//
//        for (Row row : sheet) {
//            if (row.getRowNum() == 0) continue;
//
//            try {
//                Ride ride = parseEnhancedRideRow(row, headerMap, assignmentDate);
//                if (ride != null) {
//                    rides.add(ride);
//                } else {
//                    skipped++;
//                }
//            } catch (Exception e) {
//                log.error("❌ Failed to parse row {}: {}", row.getRowNum(), e.getMessage());
//                skipped++;
//            }
//        }
//
//        workbook.close();
//        log.info("✅ Excel parsed: total={}, skipped={}", rides.size(), skipped);
//
//        ParseResult result = new ParseResult(rides, skipped, sheet.getLastRowNum());
//
//        // Optionally run optimization
//        if (runOptimization && !rides.isEmpty() && rideOptimizerService != null) {
//            log.info("🚀 Running optimization on {} parsed rides...", rides.size());
//            try {
//                rideOptimizerService.optimizeSchedule(rides);
//                result.setOptimizationRan(true);
//            } catch (Exception e) {
//                log.error("❌ Optimization failed: {}", e.getMessage());
//                result.setOptimizationError(e.getMessage());
//            }
//        }
//
//        return result;
//    }
//
//    private Map<String, Integer> buildEnhancedHeaderMapping(Sheet sheet) {
//        Map<String, Integer> headerMap = new HashMap<>();
//        Row headerRow = sheet.getRow(0);
//
//        for (Cell cell : headerRow) {
//            String header = cell.getStringCellValue().trim().toUpperCase();
//            headerMap.put(header, cell.getColumnIndex());
//
//            // Add medical transport header variations
//            addMedicalHeaderVariations(headerMap, header, cell.getColumnIndex());
//        }
//
//        log.debug("📋 Found headers: {}", String.join(", ", headerMap.keySet()));
//        return headerMap;
//    }
//
//    private void addMedicalHeaderVariations(Map<String, Integer> headerMap, String header, int columnIndex) {
//        // Map common header variations to standard names
//        switch (header) {
//            // Basic fields (your existing ones)
//            case "PATIENT NAME", "PATIENT", "CLIENT NAME", "CLIENT" ->
//                    headerMap.put("NAME", columnIndex);
//            case "PHONE NUMBER", "TELEPHONE", "CONTACT" ->
//                    headerMap.put("PHONE", columnIndex);
//            case "PICKUP LOCATION", "PICKUP ADDRESS", "FROM" ->
//                    headerMap.put("PICK UP", columnIndex);
//            case "DROPOFF LOCATION", "DROPOFF ADDRESS", "DESTINATION", "TO" ->
//                    headerMap.put("DROP OFF", columnIndex);
//            case "APPOINTMENT TIME", "PICKUP TIME", "SCHEDULED TIME" ->
//                    headerMap.put("TIME", columnIndex);
//
//            // Medical transport specific fields
//            case "WHEELCHAIR NEEDED", "WHEELCHAIR", "NEEDS WHEELCHAIR", "WC" ->
//                    headerMap.put("WHEELCHAIR", columnIndex);
//            case "STRETCHER NEEDED", "STRETCHER", "NEEDS STRETCHER", "GURNEY" ->
//                    headerMap.put("STRETCHER", columnIndex);
//            case "OXYGEN NEEDED", "OXYGEN", "NEEDS OXYGEN", "O2" ->
//                    headerMap.put("OXYGEN", columnIndex);
//            case "APPOINTMENT DURATION", "DURATION", "WAIT TIME", "APT TIME" ->
//                    headerMap.put("DURATION", columnIndex);
//            case "PRIORITY LEVEL", "PRIORITY", "URGENCY", "URGENT" ->
//                    headerMap.put("PRIORITY", columnIndex);
//            case "VEHICLE TYPE", "VEHICLE", "CAR TYPE", "TRANSPORT TYPE" ->
//                    headerMap.put("VEHICLE_TYPE", columnIndex);
//            case "EMERGENCY CONTACT", "EMERGENCY PHONE", "ICE", "EMERGENCY" ->
//                    headerMap.put("EMERGENCY_CONTACT", columnIndex);
//            case "INSURANCE", "INSURANCE PROVIDER", "INSURANCE COMPANY" ->
//                    headerMap.put("INSURANCE", columnIndex);
//            case "MEDICAL CONDITIONS", "CONDITIONS", "DIAGNOSES", "MEDICAL" ->
//                    headerMap.put("MEDICAL_CONDITIONS", columnIndex);
//            case "SPECIAL NEEDS", "NOTES", "REQUIREMENTS", "SPECIAL REQUIREMENTS" ->
//                    headerMap.put("SPECIAL_NEEDS", columnIndex);
//        }
//    }
//
//    private Ride parseEnhancedRideRow(Row row, Map<String, Integer> headerMap, LocalDate assignmentDate) {
//        // Extract basic required fields (your existing logic)
//        String name = getCellValue(row, headerMap, "NAME");
//        String phone = getCellValue(row, headerMap, "PHONE");
//        String pickup = getCellValue(row, headerMap, "PICK UP");
//        String dropoff = getCellValue(row, headerMap, "DROP OFF");
//
//        if (name.isBlank() || phone.isBlank() || pickup.isBlank() || dropoff.isBlank()) {
//            return null;
//        }
//
//        // Find or create patient with enhanced medical information
//        Patient patient = findOrCreateEnhancedPatient(row, headerMap, name, phone);
//
//        // Create ride with enhanced features
//        Ride ride = new Ride();
//        ride.setPatient(patient);
//        ride.setPickupLocation(pickup);
//        ride.setDropoffLocation(dropoff);
//
//        // Geocoding (your existing logic)
//        geocodeLocations(ride, pickup, dropoff);
//
//        // Parse time (your existing logic)
//        parsePickupTime(ride, row, headerMap, assignmentDate);
//
//        // Parse medical transport specific fields
//        parseMedicalTransportFields(ride, row, headerMap);
//
//        // Set time windows automatically (±5 minutes default)
//        setDefaultTimeWindows(ride);
//
//        // Determine vehicle requirements based on patient needs
//        setVehicleRequirements(ride);
//
//        ride.setStatus(RideStatus.SCHEDULED);
//        return rideRepository.save(ride);
//    }
//
//    private Patient findOrCreateEnhancedPatient(Row row, Map<String, Integer> headerMap, String name, String phone) {
//        Optional<Patient> existingPatient = patientRepository.findByNameAndContactInfo(name, phone);
//
//        Patient patient;
//        if (existingPatient.isPresent()) {
//            patient = existingPatient.get();
//            log.debug("📝 Found existing patient: {}", name);
//        } else {
//            patient = new Patient(name, phone);
//            log.debug("👤 Creating new patient: {}", name);
//        }
//
//        // Update/set medical information from Excel
//        updatePatientMedicalInfo(patient, row, headerMap);
//
//        return patientRepository.save(patient);
//    }
//
//    private void updatePatientMedicalInfo(Patient patient, Row row, Map<String, Integer> headerMap) {
//        // Enhanced contact info
//        patient.setPhone(getCellValue(row, headerMap, "PHONE"));
//
//        // Wheelchair requirement
//        String wheelchair = getCellValue(row, headerMap, "WHEELCHAIR");
//        if (!wheelchair.isBlank()) {
//            patient.setRequiresWheelchair(parseBoolean(wheelchair));
//        }
//
//        // Stretcher requirement
//        String stretcher = getCellValue(row, headerMap, "STRETCHER");
//        if (!stretcher.isBlank()) {
//            patient.setRequiresStretcher(parseBoolean(stretcher));
//        }
//
//        // Oxygen requirement
//        String oxygen = getCellValue(row, headerMap, "OXYGEN");
//        if (!oxygen.isBlank()) {
//            patient.setRequiresOxygen(parseBoolean(oxygen));
//        }
//
//        // Emergency contact
//        String emergencyContact = getCellValue(row, headerMap, "EMERGENCY_CONTACT");
//        if (!emergencyContact.isBlank()) {
//            if (emergencyContact.contains(":")) {
//                String[] parts = emergencyContact.split(":", 2);
//                patient.setEmergencyContactName(parts[0].trim());
//                patient.setEmergencyContactPhone(parts[1].trim());
//            } else {
//                patient.setEmergencyContactPhone(emergencyContact.trim());
//            }
//        }
//
//        // Insurance information
//        String insurance = getCellValue(row, headerMap, "INSURANCE");
//        if (!insurance.isBlank()) {
//            patient.setInsuranceProvider(insurance);
//        }
//
//        // Medical conditions
//        String conditions = getCellValue(row, headerMap, "MEDICAL_CONDITIONS");
//        if (!conditions.isBlank()) {
//            List<String> conditionList = Arrays.stream(conditions.split("[,;]"))
//                    .map(String::trim)
//                    .filter(s -> !s.isEmpty())
//                    .collect(Collectors.toList());
//            patient.setMedicalConditions(conditionList);
//        }
//
//        // Special needs
//        String specialNeeds = getCellValue(row, headerMap, "SPECIAL_NEEDS");
//        if (!specialNeeds.isBlank()) {
//            for (String need : specialNeeds.split("[,;]")) {
//                patient.addSpecialNeed(need.trim(), true);
//            }
//        }
//
//        // Determine mobility level based on requirements
//        if (Boolean.TRUE.equals(patient.getRequiresStretcher())) {
//            patient.setMobilityLevel(MobilityLevel.STRETCHER);
//        } else if (Boolean.TRUE.equals(patient.getRequiresWheelchair())) {
//            patient.setMobilityLevel(MobilityLevel.WHEELCHAIR);
//        } else {
//            patient.setMobilityLevel(MobilityLevel.INDEPENDENT);
//        }
//    }
//
//    private void parseMedicalTransportFields(Ride ride, Row row, Map<String, Integer> headerMap) {
//        // Appointment duration
//        String duration = getCellValue(row, headerMap, "DURATION");
//        if (!duration.isBlank()) {
//            try {
//                int durationMinutes = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
//                ride.setAppointmentDuration(durationMinutes);
//
//                // Set ride type based on duration
//                if (durationMinutes <= 15) {
//                    ride.setRideType(RideType.ROUND_TRIP);
//                    ride.setIsRoundTrip(true);
//                } else {
//                    ride.setRideType(RideType.ONE_WAY);
//                    ride.setIsRoundTrip(false);
//                }
//            } catch (NumberFormatException e) {
//                log.warn("⚠️ Invalid duration format: {}", duration);
//                ride.setRideType(RideType.ONE_WAY);
//                ride.setIsRoundTrip(false);
//            }
//        } else {
//            // Default to one-way if no duration specified
//            ride.setRideType(RideType.ONE_WAY);
//            ride.setIsRoundTrip(false);
//        }
//
//        // Priority level
//        String priority = getCellValue(row, headerMap, "PRIORITY");
//        if (!priority.isBlank()) {
//            if (priority.toLowerCase().contains("emergency")) {
//                ride.setPriority(Priority.EMERGENCY);
//            } else if (priority.toLowerCase().contains("urgent")) {
//                ride.setPriority(Priority.URGENT);
//            } else {
//                ride.setPriority(Priority.ROUTINE);
//            }
//        } else {
//            ride.setPriority(Priority.ROUTINE);
//        }
//
//        // Vehicle type override (if specified in Excel)
//        String vehicleType = getCellValue(row, headerMap, "VEHICLE_TYPE");
//        if (!vehicleType.isBlank()) {
//            ride.setRequiredVehicleType(vehicleType.toLowerCase().replace(" ", "_"));
//        }
//    }
//
//    private void geocodeLocations(Ride ride, String pickup, String dropoff) {
//        // Your existing geocoding logic
//        GeocodingService.GeoPoint pickupGeo = geocodingService.geocode(pickup);
//        GeocodingService.GeoPoint dropoffGeo = geocodingService.geocode(dropoff);
//
//        if (pickupGeo != null) {
//            ride.setPickupLat(pickupGeo.lat());
//            ride.setPickupLng(pickupGeo.lng());
//        }
//        if (dropoffGeo != null) {
//            ride.setDropoffLat(dropoffGeo.lat());
//            ride.setDropoffLng(dropoffGeo.lng());
//        }
//    }
//
//    private void parsePickupTime(Ride ride, Row row, Map<String, Integer> headerMap, LocalDate assignmentDate) {
//        // Your existing time parsing logic (enhanced)
//        Cell timeCell = row.getCell(headerMap.getOrDefault("TIME", -1));
//        if (timeCell == null) {
//            throw new RuntimeException("Missing TIME column");
//        }
//
//        try {
//            LocalTime time;
//
//            if (timeCell.getCellType() == CellType.NUMERIC) {
//                // Excel numeric date/time
//                Date javaDate = DateUtil.getJavaDate(timeCell.getNumericCellValue());
//                time = javaDate.toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
//            } else {
//                String timeStr = getCellValue(timeCell).trim();
//                if (timeStr.isEmpty()) {
//                    throw new RuntimeException("Empty TIME value");
//                }
//
//                if (timeStr.matches("\\d+(\\.\\d+)?")) {
//                    double numericTime = Double.parseDouble(timeStr);
//                    Date javaDate = DateUtil.getJavaDate(numericTime);
//                    time = javaDate.toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
//                } else {
//                    // Enhanced time parsing for various formats
//                    time = parseTimeString(timeStr);
//                }
//            }
//
//            ride.setPickupTime(LocalDateTime.of(assignmentDate, time));
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to parse TIME: " + e.getMessage(), e);
//        }
//    }
//
//    private LocalTime parseTimeString(String timeStr) {
//        // Handle various time formats: "9:30", "9:30 AM", "09:30", "930", etc.
//        timeStr = timeStr.toUpperCase().trim();
//
//        // Remove AM/PM and handle 12-hour format
//        boolean isPM = timeStr.contains("PM");
//        boolean isAM = timeStr.contains("AM");
//        timeStr = timeStr.replaceAll("[APM\\s]", "");
//
//        // Handle different formats
//        if (timeStr.contains(":")) {
//            String[] parts = timeStr.split(":");
//            int hour = Integer.parseInt(parts[0]);
//            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
//
//            // Convert 12-hour to 24-hour
//            if (isPM && hour != 12) hour += 12;
//            if (isAM && hour == 12) hour = 0;
//
//            return LocalTime.of(hour, minute);
//        } else {
//            // Handle formats like "930" (9:30) or "15" (3:00 PM)
//            if (timeStr.length() == 3 || timeStr.length() == 4) {
//                int hour = Integer.parseInt(timeStr.substring(0, timeStr.length() - 2));
//                int minute = Integer.parseInt(timeStr.substring(timeStr.length() - 2));
//
//                if (isPM && hour != 12) hour += 12;
//                if (isAM && hour == 12) hour = 0;
//
//                return LocalTime.of(hour, minute);
//            } else {
//                int hour = Integer.parseInt(timeStr);
//                if (isPM && hour != 12) hour += 12;
//                if (isAM && hour == 12) hour = 0;
//                return LocalTime.of(hour, 0);
//            }
//        }
//    }
//
//    private void setDefaultTimeWindows(Ride ride) {
//        if (ride.getPickupTime() != null) {
//            ride.setPickupTimeWindow(ride.getPickupTime(), 5); // ±5 minutes
//        }
//
//        // Set dropoff time if it's a round trip with appointment duration
//        if (Boolean.TRUE.equals(ride.getIsRoundTrip()) && ride.getAppointmentDuration() != null) {
//            LocalDateTime dropoffTime = ride.getPickupTime().plusMinutes(ride.getAppointmentDuration());
//            ride.setDropoffTime(dropoffTime);
//            ride.setDropoffTimeWindow(dropoffTime, 5);
//        }
//    }
//
//    private void setVehicleRequirements(Ride ride) {
//        Patient patient = ride.getPatient();
//        if (patient == null) return;
//
//        // Only set if not already specified in Excel
//        if (ride.getRequiredVehicleType() == null) {
//            if (Boolean.TRUE.equals(patient.getRequiresStretcher())) {
//                ride.setRequiredVehicleType(String.valueOf(VehicleTypeEnum.STRETCHER_VAN));
//            } else if (Boolean.TRUE.equals(patient.getRequiresWheelchair())) {
//                ride.setRequiredVehicleType(String.valueOf(VehicleTypeEnum.WHEELCHAIR_VAN));
//            } else {
//                ride.setRequiredVehicleType(String.valueOf(VehicleTypeEnum.SEDAN));
//            }
//        }
//    }
//
//    // Helper methods
//    private String getCellValue(Row row, Map<String, Integer> headerMap, String header) {
//        Integer columnIndex = headerMap.get(header);
//        if (columnIndex == null) return "";
//
//        Cell cell = row.getCell(columnIndex);
//        return getCellValue(cell);
//    }
//
//    private String getCellValue(Cell cell) {
//        // Your existing getCellValue method
//        if (cell == null) return "";
//        return switch (cell.getCellType()) {
//            case STRING -> cell.getStringCellValue().trim();
//            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
//            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
//            default -> "";
//        };
//    }
//
//    private boolean parseBoolean(String value) {
//        if (value == null || value.isBlank()) return false;
//
//        value = value.toLowerCase().trim();
//        return value.equals("true") || value.equals("yes") || value.equals("y") ||
//                value.equals("1") || value.equals("x") || value.equals("✓");
//    }
//
//    // Result class for enhanced parsing
//}