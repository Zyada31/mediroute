package com.mediroute.service.parser;

import com.mediroute.dto.*;
import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.entity.embeddable.Location;
import com.mediroute.repository.PatientRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.distance.GeocodingService;
import com.mediroute.service.distance.OsrmDistanceService;
import com.mediroute.service.ride.EnhancedMedicalTransportOptimizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import static com.mediroute.config.SecurityBeans.currentOrgId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExcelParserService {

    private final PatientRepository patientRepository;
    private final RideRepository rideRepository;
    private final GeocodingService geocodingService;
    private final OsrmDistanceService distanceService;
    private final EnhancedMedicalTransportOptimizer medicalTransportOptimizer;

    // Fixed HEADER_VARIATIONS - using proper Map.of syntax
    private static final Map<String, List<String>> HEADER_VARIATIONS = createHeaderVariations();

    private static Map<String, List<String>> createHeaderVariations() {
        Map<String, List<String>> variations = new HashMap<>();
        variations.put("NAME", Arrays.asList("NAME", "PATIENT", "PATIENT NAME", "CLIENT", "CLIENT NAME"));
        variations.put("PHONE", Arrays.asList("PHONE", "PHONE NUMBER", "TELEPHONE", "CONTACT", "MOBILE"));
        variations.put("PICK UP", Arrays.asList("PICK UP", "PICKUP", "PICKUP LOCATION", "PICKUP ADDRESS", "FROM", "ORIGIN"));
        variations.put("DROP OFF", Arrays.asList("DROP OFF", "DROPOFF", "DROP-OFF", "DROPOFF LOCATION", "DROPOFF ADDRESS", "TO", "DESTINATION"));
        variations.put("PURPOSE", Arrays.asList("PURPOSE", "REASON", "APPOINTMENT TYPE", "VISIT TYPE", "PROCEDURE"));
        variations.put("TIME", Arrays.asList("TIME", "PICKUP TIME", "APPOINTMENT TIME", "SCHEDULED TIME", "START TIME"));
        variations.put("DISTANCE", Arrays.asList("DISTANCE", "MILES", "KM", "KILOMETERS"));
        variations.put("NOTE", Arrays.asList("NOTE", "NOTES", "SPECIAL INSTRUCTIONS", "COMMENTS", "REMARKS"));
        variations.put("RUN ID", Arrays.asList("RUN ID", "RUN_ID", "BATCH", "BATCH ID", "GROUP"));
        variations.put("CANCELLED", Arrays.asList("CANCELLED", "CANCELED", "CANCEL", "STATUS"));
        variations.put("RETURN", Arrays.asList("RETURN", "ROUND TRIP", "ROUNDTRIP", "TWO WAY", "RETURN TRIP"));
        return variations;
    }

    /**
     * Enhanced parser that handles both CSV and Excel with your existing logic
     */
    public ParseResult parseExcelWithMedicalFeatures(MultipartFile file, LocalDate assignmentDate, boolean runOptimization) throws IOException {
        if (assignmentDate == null) {
            assignmentDate = LocalDate.now().plusDays(1);
        }

        log.info("🔍 Parsing file: {} for date: {}", file.getOriginalFilename(), assignmentDate);

        ParseResult result;
        String filename = file.getOriginalFilename();

        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            result = parseCsvFile(file, assignmentDate);
        } else {
            result = parseExcelFileEnhanced(file, assignmentDate);
        }

        // Enrich with geocoding and distance calculation
        enrichRidesWithLocationData(result.getRides());

        // Run optimization if requested
        if (runOptimization && !result.getRides().isEmpty()) {
            log.info("🚀 Running optimization on {} parsed rides...", result.getRides().size());
            try {
                var optimizationResult = medicalTransportOptimizer.optimizeSchedule(result.getRides());
                result.setOptimizationRan(true);
                result.setOptimizationResult(optimizationResult);
            } catch (Exception e) {
                log.error("❌ Optimization failed: {}", e.getMessage(), e);
                result.setOptimizationError(e.getMessage());
            }
        }

        log.info("✅ Parse complete: {} rides processed, {} successful, {} skipped",
                result.getTotalRows(), result.getSuccessfulRows(), result.getSkippedRows());

        return result;
    }

    /**
     * Enhanced CSV parser for your headers
     */
    private ParseResult parseCsvFile(MultipartFile file, LocalDate assignmentDate) throws IOException {
        List<Ride> rides = new ArrayList<>();
        int totalRows = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Empty CSV file");
            }

            Map<String, Integer> headerMap = buildHeaderMapping(Arrays.asList(headerLine.split(",")));
            totalRows++;

            String line;
            while ((line = reader.readLine()) != null) {
                totalRows++;
                try {
                    String[] values = parseCsvLine(line);
                    Ride ride = parseRideFromCsvValues(values, headerMap, assignmentDate);
                    if (ride != null) {
                        rides.add(ride);
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to parse CSV row {}: {}", totalRows, e.getMessage());
                    skipped++;
                }
            }
        }

        return ParseResult.create(rides, skipped, totalRows);
    }

    /**
     * Enhanced Excel parser using your existing logic
     */
    private ParseResult parseExcelFileEnhanced(MultipartFile file, LocalDate assignmentDate) throws IOException {
        List<Ride> rides = new ArrayList<>();
        int totalRows = 0;
        int skipped = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum() + 1;

            if (totalRows == 0) {
                throw new IllegalArgumentException("Empty Excel file");
            }

            // Build header mapping using your existing method
            Map<String, Integer> headerMap = buildEnhancedHeaderMapping(sheet);

            // Process data rows using your existing logic
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Ride ride = parseEnhancedRideRow(row, headerMap, assignmentDate);
                    if (ride != null) {
                        rides.add(ride);
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to parse Excel row {}: {}", i + 1, e.getMessage());
                    skipped++;
                }
            }
        }

        return ParseResult.create(rides, skipped, totalRows);
    }

    /**
     * Parse ride from CSV values matching your header spec
     */
    private Ride parseRideFromCsvValues(String[] values, Map<String, Integer> headerMap, LocalDate assignmentDate) {
        try {
            // Extract required fields from your CSV format
            String name = getValue(values, headerMap, "NAME");
            String phone = getValue(values, headerMap, "PHONE");
            String pickupLocation = getValue(values, headerMap, "PICK UP");
            String dropoffLocation = getValue(values, headerMap, "DROP OFF");
            String purpose = getValue(values, headerMap, "PURPOSE");
            String timeStr = getValue(values, headerMap, "TIME");
            String cancelled = getValue(values, headerMap, "CANCELLED");
            String returnTrip = getValue(values, headerMap, "RETURN");
            String runId = getValue(values, headerMap, "RUN ID");
            String notes = getValue(values, headerMap, "NOTE");
            String distanceStr = getValue(values, headerMap, "DISTANCE");

            // Validate required fields
            if (isBlank(name) || isBlank(phone) || isBlank(pickupLocation) || isBlank(dropoffLocation) || isBlank(timeStr)) {
                log.warn("⚠️ Skipping row with missing required fields");
                return null;
            }

            // Check if cancelled
            if (!isBlank(cancelled) && parseBoolean(cancelled)) {
                log.debug("🚫 Skipping cancelled ride for {}", name);
                return null;
            }

            // Find or create patient
        Patient patient = findOrCreatePatient(name, phone);

            // Parse pickup time
            LocalDateTime pickupTime = parseDateTime(timeStr, assignmentDate);
            if (pickupTime == null) {
                log.warn("⚠️ Invalid time format for {}: {}", name, timeStr);
                return null;
            }

            // Create ride with your embeddable Location
            Ride ride = Ride.builder()
                    .patient(patient)
                    .pickupLocation(createLocation(pickupLocation))
                    .dropoffLocation(createLocation(dropoffLocation))
                    .pickupTime(pickupTime)
                    .priority(determinePriority(purpose))
                    .status(RideStatus.SCHEDULED)
                    .build();
            Long org = currentOrgId();
            if (org != null) {
                ride.setOrgId(org);
            }

            // Handle return trip logic
            boolean isRoundTrip = !isBlank(returnTrip) && parseBoolean(returnTrip);
            ride.setIsRoundTrip(isRoundTrip);
            ride.setRideType(isRoundTrip ? RideType.ROUND_TRIP : RideType.ONE_WAY);

            // Set appointment duration based on purpose if round trip
            if (isRoundTrip) {
                Integer duration = determineAppointmentDuration(purpose);
                ride.setAppointmentDuration(duration);
                if (duration != null) {
                    LocalDateTime dropoffTime = pickupTime.plusMinutes(duration);
                    ride.setDropoffTime(dropoffTime);
                    ride.setDropoffTimeWindow(dropoffTime, 5);
                }
            }

            // Set time windows (±5 minutes default)
            ride.setPickupTimeWindow(pickupTime, 5);
            // Unassigned initially means SCHEDULED (no driver assigned yet)
            ride.setStatus(RideStatus.SCHEDULED);

            // Handle notes and special requirements
            if (!isBlank(notes)) {
                patient.addSpecialNeed("notes", notes);
                analyzeNotesForMedicalRequirements(patient, notes);
            }

            // Parse distance if provided
            if (!isBlank(distanceStr)) {
                try {
                    double distance = Double.parseDouble(distanceStr.replaceAll("[^0-9.]", ""));
                    ride.setDistance(distance);
                } catch (NumberFormatException e) {
                    log.debug("Could not parse distance: {}", distanceStr);
                }
            }

            // Set run ID for batch tracking
            if (!isBlank(runId)) {
                ride.setOptimizationBatchId(runId);
            }

            return rideRepository.save(ride);

        } catch (Exception e) {
            log.error("❌ Failed to parse ride from CSV values", e);
            return null;
        }
    }

    /**
     * Use your existing parseEnhancedRideRow method but with Location embeddable
     */
    private Ride parseEnhancedRideRow(Row row, Map<String, Integer> headerMap, LocalDate assignmentDate) {
        // Extract basic required fields
        String name = getCellValue(row, headerMap, "NAME");
        String phone = getCellValue(row, headerMap, "PHONE");
        String pickup = getCellValue(row, headerMap, "PICK UP");
        String dropoff = getCellValue(row, headerMap, "DROP OFF");

        if (name.isBlank() || phone.isBlank() || pickup.isBlank() || dropoff.isBlank()) {
            return null;
        }

        // Find or create patient with enhanced medical information
        Patient patient = findOrCreateEnhancedPatient(row, headerMap, name, phone);

        // Create ride with enhanced features using embeddable Location
        Ride ride = Ride.builder()
                .patient(patient)
                .pickupLocation(createLocation(pickup))
                .dropoffLocation(createLocation(dropoff))
                .status(RideStatus.SCHEDULED)
                .build();
        Long org = currentOrgId();
        if (org != null) {
            ride.setOrgId(org);
        }

        // Parse time
        parsePickupTime(ride, row, headerMap, assignmentDate);

        // Parse medical transport specific fields
        parseMedicalTransportFields(ride, row, headerMap);

        // Set time windows automatically and ensure unassigned status
        setDefaultTimeWindows(ride);
        ride.setStatus(RideStatus.SCHEDULED);

        // Determine vehicle requirements based on patient needs
        setVehicleRequirements(ride);

        return rideRepository.save(ride);
    }

    // Rest of the helper methods remain the same...
    private Map<String, Integer> buildHeaderMapping(List<String> headers) {
        Map<String, Integer> headerMap = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).trim().toUpperCase();
            headerMap.put(header, i);

            // Map variations to standard names
            for (Map.Entry<String, List<String>> entry : HEADER_VARIATIONS.entrySet()) {
                if (entry.getValue().contains(header)) {
                    headerMap.put(entry.getKey(), i);
                    break;
                }
            }
        }

        log.debug("📋 Header mapping: {}", headerMap.keySet());
        return headerMap;
    }

    private Map<String, Integer> buildEnhancedHeaderMapping(Sheet sheet) {
        Map<String, Integer> headerMap = new HashMap<>();
        Row headerRow = sheet.getRow(0);

        if (headerRow == null) {
            throw new IllegalArgumentException("No header row found in Excel file");
        }

        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim().toUpperCase();
            headerMap.put(header, cell.getColumnIndex());

            // Add medical header variations
            addMedicalHeaderVariations(headerMap, header, cell.getColumnIndex());

            // Add CSV header variations
            addCsvHeaderVariations(headerMap, header, cell.getColumnIndex());
        }

        log.debug("📋 Found headers: {}", String.join(", ", headerMap.keySet()));
        return headerMap;
    }

    private void addCsvHeaderVariations(Map<String, Integer> headerMap, String header, int columnIndex) {
        // Map CSV header variations to standard names
        for (Map.Entry<String, List<String>> entry : HEADER_VARIATIONS.entrySet()) {
            if (entry.getValue().contains(header)) {
                headerMap.put(entry.getKey(), columnIndex);
                break;
            }
        }
    }

    private void addMedicalHeaderVariations(Map<String, Integer> headerMap, String header, int columnIndex) {
        // Medical header variations
        switch (header) {
            case "PATIENT NAME", "PATIENT", "CLIENT NAME", "CLIENT" ->
                    headerMap.put("NAME", columnIndex);
            case "PHONE NUMBER", "TELEPHONE", "CONTACT" ->
                    headerMap.put("PHONE", columnIndex);
            case "PICKUP LOCATION", "PICKUP ADDRESS", "FROM" ->
                    headerMap.put("PICK UP", columnIndex);
            case "DROPOFF LOCATION", "DROPOFF ADDRESS", "DESTINATION", "TO" ->
                    headerMap.put("DROP OFF", columnIndex);
            case "APPOINTMENT TIME", "PICKUP TIME", "SCHEDULED TIME" ->
                    headerMap.put("TIME", columnIndex);
            case "WHEELCHAIR NEEDED", "WHEELCHAIR", "NEEDS WHEELCHAIR", "WC" ->
                    headerMap.put("WHEELCHAIR", columnIndex);
            case "STRETCHER NEEDED", "STRETCHER", "NEEDS STRETCHER", "GURNEY" ->
                    headerMap.put("STRETCHER", columnIndex);
            case "OXYGEN NEEDED", "OXYGEN", "NEEDS OXYGEN", "O2" ->
                    headerMap.put("OXYGEN", columnIndex);
        }
    }

    private Location createLocation(String address) {
        Location location = new Location();
        location.setAddress(address);
        return location;
    }

    private Patient findOrCreatePatient(String name, String phone) {
        Long org = currentOrgId();
        Optional<Patient> existingPatient = (org != null)
                ? patientRepository.findByPhoneAndOrgId(phone, org)
                : patientRepository.findByPhone(phone);

        if (existingPatient.isPresent()) {
            log.debug("👤 Found existing patient: {}", name);
            return existingPatient.get();
        }

        log.debug("👤 Creating new patient: {}", name);
        Patient patient = Patient.builder()
                .name(name)
                .phone(phone)
                .isActive(true)
                .build();
        if (org != null) patient.setOrgId(org);

        return patientRepository.save(patient);
    }

    private Patient findOrCreateEnhancedPatient(Row row, Map<String, Integer> headerMap, String name, String phone) {
        Long org = currentOrgId();
        Optional<Patient> existingPatient = (org != null)
                ? patientRepository.findByPhoneAndOrgId(phone, org)
                : patientRepository.findByPhone(phone);

        Patient patient;
        if (existingPatient.isPresent()) {
            patient = existingPatient.get();
            log.debug("📝 Found existing patient: {}", name);
        } else {
            patient = Patient.builder()
                    .name(name)
                    .phone(phone)
                    .isActive(true)
                    .build();
            if (org != null) patient.setOrgId(org);
            log.debug("👤 Creating new patient: {}", name);
        }

        // Update/set medical information from Excel
        updatePatientMedicalInfo(patient, row, headerMap);

        return patientRepository.save(patient);
    }

    // All other helper methods remain the same...
    // (Including: parseDateTime, determineAppointmentDuration, etc.)

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    private String getValue(String[] values, Map<String, Integer> headerMap, String key) {
        Integer index = headerMap.get(key);
        if (index == null || index >= values.length) {
            return "";
        }
        return values[index] != null ? values[index].trim() : "";
    }

    private Priority determinePriority(String purpose) {
        if (isBlank(purpose)) {
            return Priority.ROUTINE;
        }

        String purposeLower = purpose.toLowerCase();
        if (purposeLower.contains("emergency") || purposeLower.contains("urgent") || purposeLower.contains("stat")) {
            return Priority.EMERGENCY;
        }
        if (purposeLower.contains("dialysis") || purposeLower.contains("chemo") || purposeLower.contains("radiation")) {
            return Priority.URGENT;
        }
        return Priority.ROUTINE;
    }

    private Integer determineAppointmentDuration(String purpose) {
        if (isBlank(purpose)) {
            return 60; // Default 1 hour
        }

        String purposeLower = purpose.toLowerCase();
        if (purposeLower.contains("dialysis")) {
            return 240; // 4 hours
        }
        if (purposeLower.contains("chemo") || purposeLower.contains("chemotherapy")) {
            return 180; // 3 hours
        }
        if (purposeLower.contains("radiation")) {
            return 30; // 30 minutes
        }
        return 60; // Default 1 hour
    }

    private LocalDateTime parseDateTime(String timeStr, LocalDate assignmentDate) {
        if (isBlank(timeStr)) return null;

        try {
            String cleanTime = timeStr.trim().toUpperCase();
            cleanTime = cleanTime.replaceAll("^(AT|@)\\s*", "");

            LocalTime time = null;

            // Try parsing various formats
            List<DateTimeFormatter> formatters = Arrays.asList(
                    DateTimeFormatter.ofPattern("H:mm"),
                    DateTimeFormatter.ofPattern("HH:mm"),
                    DateTimeFormatter.ofPattern("h:mm a"),
                    DateTimeFormatter.ofPattern("H:mm a")
            );

            for (DateTimeFormatter formatter : formatters) {
                try {
                    time = LocalTime.parse(cleanTime, formatter);
                    break;
                } catch (DateTimeParseException ignored) {
                    // Try next formatter
                }
            }

            if (time == null) {
                time = parseTimeManually(cleanTime);
            }

            return time != null ? LocalDateTime.of(assignmentDate, time) : null;

        } catch (Exception e) {
            log.debug("Failed to parse time: {}", timeStr, e);
            return null;
        }
    }

    private LocalTime parseTimeManually(String timeStr) {
        try {
            String cleaned = timeStr.replaceAll("[^0-9APM]", "");
            boolean isPM = cleaned.contains("P");
            boolean isAM = cleaned.contains("A");
            String numbers = cleaned.replaceAll("[APM]", "");

            if (numbers.length() == 3 || numbers.length() == 4) {
                int hour, minute;
                if (numbers.length() == 3) {
                    hour = Integer.parseInt(numbers.substring(0, 1));
                    minute = Integer.parseInt(numbers.substring(1));
                } else {
                    hour = Integer.parseInt(numbers.substring(0, 2));
                    minute = Integer.parseInt(numbers.substring(2));
                }

                // Convert 12-hour to 24-hour
                if (isPM && hour != 12) hour += 12;
                if (isAM && hour == 12) hour = 0;

                return LocalTime.of(hour, minute);
            }
        } catch (Exception e) {
            log.debug("Manual time parsing failed for: {}", timeStr, e);
        }
        return null;
    }

    private boolean parseBoolean(String value) {
        if (isBlank(value)) return false;
        String lower = value.toLowerCase().trim();
        return "true".equals(lower) || "yes".equals(lower) || "y".equals(lower) ||
                "1".equals(lower) || "x".equals(lower) || "✓".equals(lower);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean hasValidCoordinates(Ride ride) {
        return ride.getPickupLocation() != null &&
                ride.getPickupLocation().isValid() &&
                ride.getDropoffLocation() != null &&
                ride.getDropoffLocation().isValid();
    }

    private void enrichRidesWithLocationData(List<Ride> rides) {
        log.info("🌍 Enriching {} rides with location data...", rides.size());

        for (Ride ride : rides) {
            try {
                // Geocode pickup location
                if (ride.getPickupLocation() != null && ride.getPickupLocation().getAddress() != null) {
                    GeocodingService.GeoPoint pickupGeo = geocodingService.geocode(ride.getPickupLocation().getAddress());
                    if (pickupGeo != null) {
                        ride.getPickupLocation().setLatitude(pickupGeo.lat());
                        ride.getPickupLocation().setLongitude(pickupGeo.lng());
                    }
                }

                // Geocode dropoff location
                if (ride.getDropoffLocation() != null && ride.getDropoffLocation().getAddress() != null) {
                    GeocodingService.GeoPoint dropoffGeo = geocodingService.geocode(ride.getDropoffLocation().getAddress());
                    if (dropoffGeo != null) {
                        ride.getDropoffLocation().setLatitude(dropoffGeo.lat());
                        ride.getDropoffLocation().setLongitude(dropoffGeo.lng());
                    }
                }

                // Calculate distance and duration if coordinates are available
                if (hasValidCoordinates(ride)) {
                    calculateDistanceAndDuration(ride);
                }

                rideRepository.save(ride);

            } catch (Exception e) {
                log.error("❌ Failed to enrich ride {} with location data", ride.getId(), e);
            }
        }

        log.info("✅ Location enrichment complete");
    }

    private void calculateDistanceAndDuration(Ride ride) {
        try {
            if (!hasValidCoordinates(ride)) {
                log.warn("⚠️ Cannot calculate distance - invalid coordinates for ride {}", ride.getId());
                return;
            }

            String origin = ride.getPickupLocation().toOsrmFormat();
            String destination = ride.getDropoffLocation().toOsrmFormat();

            int distanceMeters = distanceService.getDistanceInMeters(origin, destination);
            double distanceKm = distanceMeters / 1000.0;

            ride.setDistance(distanceKm);

            // Estimate duration (urban areas ~25 km/h average with stops)
            int estimatedMinutes = (int) Math.ceil((distanceKm / 25.0) * 60);
            ride.setEstimatedDuration(estimatedMinutes);

            log.debug("📏 Ride {}: {}km, ~{}min", ride.getId(),
                    String.format("%.2f", distanceKm), estimatedMinutes);

        } catch (Exception e) {
            log.error("❌ Failed to calculate distance for ride {}", ride.getId(), e);
        }
    }

    private void analyzeNotesForMedicalRequirements(Patient patient, String notes) {
        if (isBlank(notes)) return;

        String notesLower = notes.toLowerCase();

        // Check for wheelchair requirements
        if (notesLower.contains("wheelchair") || notesLower.contains("w/c") || notesLower.contains("wc")) {
            patient.setRequiresWheelchair(true);
            patient.setMobilityLevel(MobilityLevel.WHEELCHAIR);
        }

        // Check for stretcher requirements
        if (notesLower.contains("stretcher") || notesLower.contains("gurney") || notesLower.contains("bed bound")) {
            patient.setRequiresStretcher(true);
            patient.setMobilityLevel(MobilityLevel.STRETCHER);
        }

        // Check for oxygen requirements
        if (notesLower.contains("oxygen") || notesLower.contains("o2") || notesLower.contains("breathing")) {
            patient.setRequiresOxygen(true);
        }

        // Add medical conditions based on keywords
        if (notesLower.contains("dialysis")) {
            patient.addMedicalCondition("dialysis");
        }
        if (notesLower.contains("chemo") || notesLower.contains("chemotherapy")) {
            patient.addMedicalCondition("chemotherapy");
        }
    }

    // Placeholder methods - implement as needed
    private void parsePickupTime(Ride ride, Row row, Map<String, Integer> headerMap, LocalDate assignmentDate) {
        // Your existing time parsing logic (enhanced)
        Cell timeCell = row.getCell(headerMap.getOrDefault("TIME", -1));
        if (timeCell == null) {
            throw new RuntimeException("Missing TIME column");
        }

        try {
            LocalTime time;

            if (timeCell.getCellType() == CellType.NUMERIC) {
                // Excel numeric date/time
                Date javaDate = DateUtil.getJavaDate(timeCell.getNumericCellValue());
                time = javaDate.toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
            } else {
                String timeStr = getCellValue(timeCell).trim();
                if (timeStr.isEmpty()) {
                    throw new RuntimeException("Empty TIME value");
                }

                if (timeStr.matches("\\d+(\\.\\d+)?")) {
                    double numericTime = Double.parseDouble(timeStr);
                    Date javaDate = DateUtil.getJavaDate(numericTime);
                    time = javaDate.toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
                } else {
                    // Enhanced time parsing for various formats
                    time = parseTimeString(timeStr);
                }
            }

            ride.setPickupTime(LocalDateTime.of(assignmentDate, time));

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse TIME: " + e.getMessage(), e);
        }
    }
        private LocalTime parseTimeString(String timeStr) {
        // Handle various time formats: "9:30", "9:30 AM", "09:30", "930", etc.
        timeStr = timeStr.toUpperCase().trim();

        // Remove AM/PM and handle 12-hour format
        boolean isPM = timeStr.contains("PM");
        boolean isAM = timeStr.contains("AM");
        timeStr = timeStr.replaceAll("[APM\\s]", "");

        // Handle different formats
        if (timeStr.contains(":")) {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            // Convert 12-hour to 24-hour
            if (isPM && hour != 12) hour += 12;
            if (isAM && hour == 12) hour = 0;

            return LocalTime.of(hour, minute);
        } else {
            // Handle formats like "930" (9:30) or "15" (3:00 PM)
            if (timeStr.length() == 3 || timeStr.length() == 4) {
                int hour = Integer.parseInt(timeStr.substring(0, timeStr.length() - 2));
                int minute = Integer.parseInt(timeStr.substring(timeStr.length() - 2));

                if (isPM && hour != 12) hour += 12;
                if (isAM && hour == 12) hour = 0;

                return LocalTime.of(hour, minute);
            } else {
                int hour = Integer.parseInt(timeStr);
                if (isPM && hour != 12) hour += 12;
                if (isAM && hour == 12) hour = 0;
                return LocalTime.of(hour, 0);
            }
        }
    }
        private void parseMedicalTransportFields(Ride ride, Row row, Map<String, Integer> headerMap) {
        // Appointment duration
        String duration = getCellValue(row, headerMap, "DURATION");
        if (!duration.isBlank()) {
            try {
                int durationMinutes = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
                ride.setAppointmentDuration(durationMinutes);

                // Set ride type based on duration
                if (durationMinutes <= 15) {
                    ride.setRideType(RideType.ROUND_TRIP);
                    ride.setIsRoundTrip(true);
                } else {
                    ride.setRideType(RideType.ONE_WAY);
                    ride.setIsRoundTrip(false);
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ Invalid duration format: {}", duration);
                ride.setRideType(RideType.ONE_WAY);
                ride.setIsRoundTrip(false);
            }
        } else {
            // Default to one-way if no duration specified
            ride.setRideType(RideType.ONE_WAY);
            ride.setIsRoundTrip(false);
        }

        // Priority level
        String priority = getCellValue(row, headerMap, "PRIORITY");
        if (!priority.isBlank()) {
            if (priority.toLowerCase().contains("emergency")) {
                ride.setPriority(Priority.EMERGENCY);
            } else if (priority.toLowerCase().contains("urgent")) {
                ride.setPriority(Priority.URGENT);
            } else {
                ride.setPriority(Priority.ROUTINE);
            }
        } else {
            ride.setPriority(Priority.ROUTINE);
        }

        // Vehicle type override (if specified in Excel)
        String vehicleType = getCellValue(row, headerMap, "VEHICLE_TYPE");
        if (!vehicleType.isBlank()) {
            ride.setRequiredVehicleType(vehicleType.toLowerCase().replace(" ", "_"));
        }
    }
    private void setDefaultTimeWindows(Ride ride) {
        if (ride.getPickupTime() != null) {
            ride.setPickupTimeWindow(ride.getPickupTime(), 5);
        }
    }

    private void setVehicleRequirements(Ride ride) {
        Patient patient = ride.getPatient();
        if (patient == null) return;

        // Only set if not already specified in Excel
        if (ride.getRequiredVehicleType() == null) {
            if (Boolean.TRUE.equals(patient.getRequiresStretcher())) {
                ride.setRequiredVehicleType(String.valueOf(VehicleTypeEnum.STRETCHER_VAN));
            } else if (Boolean.TRUE.equals(patient.getRequiresWheelchair())) {
                ride.setRequiredVehicleType(String.valueOf(VehicleTypeEnum.WHEELCHAIR_VAN));
            } else {
                ride.setRequiredVehicleType(String.valueOf(VehicleTypeEnum.SEDAN));
            }
        }
    }

    private void updatePatientMedicalInfo(Patient patient, Row row, Map<String, Integer> headerMap) {
        // Enhanced contact info
        patient.setPhone(getCellValue(row, headerMap, "PHONE"));

        // Wheelchair requirement
        String wheelchair = getCellValue(row, headerMap, "WHEELCHAIR");
        if (!wheelchair.isBlank()) {
            patient.setRequiresWheelchair(parseBoolean(wheelchair));
        }

        // Stretcher requirement
        String stretcher = getCellValue(row, headerMap, "STRETCHER");
        if (!stretcher.isBlank()) {
            patient.setRequiresStretcher(parseBoolean(stretcher));
        }

        // Oxygen requirement
        String oxygen = getCellValue(row, headerMap, "OXYGEN");
        if (!oxygen.isBlank()) {
            patient.setRequiresOxygen(parseBoolean(oxygen));
        }

        // Emergency contact
        String emergencyContact = getCellValue(row, headerMap, "EMERGENCY_CONTACT");
        if (!emergencyContact.isBlank()) {
            if (emergencyContact.contains(":")) {
                String[] parts = emergencyContact.split(":", 2);
                patient.setEmergencyContactName(parts[0].trim());
                patient.setEmergencyContactPhone(parts[1].trim());
            } else {
                patient.setEmergencyContactPhone(emergencyContact.trim());
            }
        }

        // Insurance information
        String insurance = getCellValue(row, headerMap, "INSURANCE");
        if (!insurance.isBlank()) {
            patient.setInsuranceProvider(insurance);
        }

        // Medical conditions
        String conditions = getCellValue(row, headerMap, "MEDICAL_CONDITIONS");
        if (!conditions.isBlank()) {
            List<String> conditionList = Arrays.stream(conditions.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            patient.setMedicalConditions(conditionList);
        }

        // Special needs
        String specialNeeds = getCellValue(row, headerMap, "SPECIAL_NEEDS");
        if (!specialNeeds.isBlank()) {
            for (String need : specialNeeds.split("[,;]")) {
                patient.addSpecialNeed(need.trim(), true);
            }
        }

        // Determine mobility level based on requirements
        if (Boolean.TRUE.equals(patient.getRequiresStretcher())) {
            patient.setMobilityLevel(MobilityLevel.STRETCHER);
        } else if (Boolean.TRUE.equals(patient.getRequiresWheelchair())) {
            patient.setMobilityLevel(MobilityLevel.WHEELCHAIR);
        } else {
            patient.setMobilityLevel(MobilityLevel.INDEPENDENT);
        }
    }

    private String getCellValue(Row row, Map<String, Integer> headerMap, String header) {
        Integer columnIndex = headerMap.get(header);
        if (columnIndex == null) return "";

        Cell cell = row.getCell(columnIndex);
        return getCellValue(cell);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        // Use DataFormatter to preserve human-readable content (avoids scientific notation on phones)
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}