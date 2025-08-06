//package com.mediroute.service.parser;
//
//import com.mediroute.dto.RideStatus;
//import com.mediroute.entity.Patient;
//import com.mediroute.entity.Ride;
//import com.mediroute.repository.PatientRepository;
//import com.mediroute.repository.RideRepository;
//import com.mediroute.service.distance.GeocodingService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.poi.ss.usermodel.*;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.time.*;
//import java.util.*;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class ExcelRideParser {
//
//    private final PatientRepository patientRepository;
//    private final RideRepository rideRepository;
//    private final GeocodingService geocodingService;
//
//    public List<Ride> parseExcel(MultipartFile file, LocalDate assignmentDate) throws IOException {
//        if (assignmentDate == null) assignmentDate = LocalDate.now().plusDays(1);
//
//        Workbook workbook = WorkbookFactory.create(file.getInputStream());
//        Sheet sheet = workbook.getSheetAt(0);
//
//        Map<String, Integer> headerMap = new HashMap<>();
//        Row headerRow = sheet.getRow(0);
//        for (Cell cell : headerRow) {
//            headerMap.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
//        }
//
//        List<Ride> rides = new ArrayList<>();
//        int skipped = 0;
//
//        for (Row row : sheet) {
//            if (row.getRowNum() == 0) continue;
//
//            String name = getCellValue(row.getCell(headerMap.get("NAME")));
//            String phone = getCellValue(row.getCell(headerMap.get("PHONE")));
//            String pickup = getCellValue(row.getCell(headerMap.get("PICK UP")));
//            String dropoff = getCellValue(row.getCell(headerMap.get("DROP OFF")));
////            String timeStr = getCellValue(row.getCell(headerMap.get("TIME")));
//
//            if (name.isBlank() || phone.isBlank() || pickup.isBlank() || dropoff.isBlank() ) {
//                skipped++;
//                continue;
//            }
//
//            Patient patient = patientRepository.findByNameAndContactInfo(name, phone)
//                    .orElse(new Patient(name, phone));
//            patient = patientRepository.save(patient);
//
//            Ride ride = new Ride();
//            ride.setPatient(patient);
//            ride.setPickupLocation(pickup);
//            ride.setDropoffLocation(dropoff);
//
//            // Geocode pickup & dropoff
//            GeocodingService.GeoPoint pickupGeo = geocodingService.geocode(pickup);
//            GeocodingService.GeoPoint dropoffGeo = geocodingService.geocode(dropoff);
//
//            if (pickupGeo != null) {
//                ride.setPickupLat(pickupGeo.lat());
//                ride.setPickupLng(pickupGeo.lng());
//            }
//            if (dropoffGeo != null) {
//                ride.setDropoffLat(dropoffGeo.lat());
//                ride.setDropoffLng(dropoffGeo.lng());
//            }
//
//            // Parse TIME
//            Cell timeCell = row.getCell(headerMap.getOrDefault("TIME", 5));
//            if (timeCell == null) {
//                skipped++;
//                continue;
//            }
//
//            try {
//                LocalTime time;
//
//                if (timeCell.getCellType() == CellType.NUMERIC) {
//                    // Excel numeric date/time
//                    Date javaDate = DateUtil.getJavaDate(timeCell.getNumericCellValue());
//                    time = javaDate.toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
//                } else {
//                    String timeStr = getCellValue(timeCell).trim();
//                    if (timeStr.isEmpty()) {
//                        skipped++;
//                        continue;
//                    }
//                    if (timeStr.matches("\\d+(\\.\\d+)?")) {
//                        double numericTime = Double.parseDouble(timeStr);
//                        Date javaDate = DateUtil.getJavaDate(numericTime);
//                        time = javaDate.toInstant().atZone(ZoneId.of("America/Denver")).toLocalTime();
//                    } else {
//                        String[] parts = timeStr.split(":");
//                        int hour = Integer.parseInt(parts[0].trim());
//                        int minute = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : 0;
//                        time = LocalTime.of(hour, minute);
//                    }
//                }
//
//                ride.setPickupTime(LocalDateTime.of(assignmentDate, time));
//            } catch (Exception e) {
//                log.error("❌ Failed to parse TIME at row {}: {}", row.getRowNum(), e.getMessage());
//                skipped++;
//                continue;
//            }
//
////            ride.setPickupTime(LocalDateTime.of(assignmentDate, localTime));
//
//            ride.setStatus(RideStatus.SCHEDULED);
//            rides.add(rideRepository.save(ride));
//        }
//
//        log.info("✅ Excel parsed: total={}, skipped={}", rides.size(), skipped);
//        workbook.close();
//        return rides;
//    }
//
//    private String getCellValue(Cell cell) {
//        if (cell == null) return "";
//        return switch (cell.getCellType()) {
//            case STRING -> cell.getStringCellValue().trim();
//            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
//            default -> "";
//        };
//    }
//}