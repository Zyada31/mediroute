package com.mediroute.parser;

import com.mediroute.entity.Patient;
import com.mediroute.entity.Ride;
import com.mediroute.repository.PatientRepository;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelRideParser {

    private final PatientRepository patientRepository;
    private final RideRepository rideRepository;

    public List<Ride> parseExcel(MultipartFile file, LocalDate assignmentDate) throws IOException {
        if (assignmentDate == null) assignmentDate = LocalDate.now().plusDays(1);

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Map<String, Integer> headerMap = new HashMap<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                headerMap.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
            }
        }

        List<Ride> rides = new ArrayList<>();
        int skippedBlank = 0, skippedCancelled = 0, skippedTime = 0, skippedRequired = 0, returnRideCount = 0;

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            if (row == null || row.getLastCellNum() < 1) {
                skippedBlank++;
                continue;
            }

            String cancelled = getCellValue(row.getCell(headerMap.getOrDefault("CANCELLED", 9))).trim().toUpperCase();
            if ("YES".equals(cancelled) || cancelled.isEmpty()) {
                skippedCancelled++;
                continue;
            }

            String name = getCellValue(row.getCell(headerMap.getOrDefault("NAME", 0))).trim();
            String phone = getCellValue(row.getCell(headerMap.getOrDefault("PHONE", 1))).trim();
            String pickup = getCellValue(row.getCell(headerMap.getOrDefault("PICK UP", 2))).trim();
            String dropoff = getCellValue(row.getCell(headerMap.getOrDefault("DROP OFF", 3))).trim();
            String distanceStr = getCellValue(row.getCell(headerMap.getOrDefault("DISTANCE", 6))).trim();
            if (name.isEmpty() || phone.isEmpty() || pickup.isEmpty() || dropoff.isEmpty() || distanceStr.isEmpty()) {
                skippedRequired++;
                continue;
            }

            Patient patient = patientRepository.findByNameAndContactInfo(name, phone).orElse(new Patient());
            patient.setName(name);
            patient.setContactInfo(phone);
            patient.setDefaultPickupLocation(pickup);
            patient.setDefaultDropoffLocation(dropoff);
            Map<String, Object> specialNeeds = new HashMap<>(Optional.ofNullable(patient.getSpecialNeeds()).orElse(new HashMap<>()));
            specialNeeds.put("purpose", getCellValue(row.getCell(headerMap.getOrDefault("PURPOSE", 4))).trim());
            specialNeeds.put("note", getCellValue(row.getCell(headerMap.getOrDefault("NOTE", 7))).trim());
            String returnTrip = getCellValue(row.getCell(headerMap.getOrDefault("RETURN", 11))).trim().toUpperCase();
            specialNeeds.put("return", returnTrip);
            patient.setSpecialNeeds(specialNeeds);
            patient = patientRepository.save(patient);

            Ride ride = new Ride();
            ride.setPatient(patient);
            ride.setPickupLocation(pickup);
            ride.setDropoffLocation(dropoff);
            ride.setStatus("scheduled");
            ride.setRequiredVehicleType("sedan");
            try {
                ride.setDistance(Float.parseFloat(distanceStr));
            } catch (NumberFormatException e) {
                skippedRequired++;
                continue;
            }

            // Parse SKILLS into List<String>
            String rawSkills = getCellValue(row.getCell(headerMap.getOrDefault("SKILLS", 12))).trim();
            List<String> requiredSkills = Arrays.stream(rawSkills.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            ride.setRequiredSkills(requiredSkills);

            // Parse TIME
            String timeStr = getCellValue(row.getCell(headerMap.getOrDefault("TIME", 5))).trim();
            if (timeStr.isEmpty()) {
                skippedTime++;
                continue;
            }
            try {
                String formattedTime;
                if (timeStr.matches("\\d+(\\.\\d+)?")) {
                    double numericTime = Double.parseDouble(timeStr);
                    LocalTime time = DateUtil.getJavaDate(numericTime).toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
                    formattedTime = time.toString();
                } else {
                    String[] parts = timeStr.split(":");
                    String hh = parts[0].trim();
                    String mm = (parts.length > 1) ? parts[1].trim() : "00";
                    formattedTime = String.format("%02d:%02d", Integer.parseInt(hh), Integer.parseInt(mm));
                }
                ride.setPickupTime(LocalDateTime.parse(assignmentDate + "T" + formattedTime + ":00"));
            } catch (Exception e) {
                skippedTime++;
                continue;
            }

            ride = rideRepository.save(ride);
            rides.add(ride);

            if ("YES".equals(returnTrip)) {
                Ride returnRide = new Ride();
                returnRide.setPatient(patient);
                returnRide.setPickupLocation(dropoff);
                returnRide.setDropoffLocation(pickup);
                returnRide.setPickupTime(ride.getPickupTime().plusHours(1));
                returnRide.setDistance(ride.getDistance());
                returnRide.setStatus("scheduled");
                returnRide.setRequiredVehicleType(ride.getRequiredVehicleType());
                returnRide.setRequiredSkills(ride.getRequiredSkills());
                rides.add(rideRepository.save(returnRide));
                returnRideCount++;
            }
        }

        workbook.close();
        log.info("✅ Excel parsed: total={}, return={}, blank={}, cancelled={}, time_missing={}, required_missing={}",
                rides.size(), returnRideCount, skippedBlank, skippedCancelled, skippedTime, skippedRequired);
        return rides;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ?
                    cell.getLocalDateTimeCellValue().toLocalTime().toString() :
                    String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}