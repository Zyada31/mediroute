//package com.mediroute.unit.service;
//
//import com.google.ortools.Loader;
//import com.mediroute.entity.Patient;
//import com.mediroute.entity.Ride;
//import com.mediroute.repository.PatientRepository;
//import com.mediroute.repository.RideRepository;
//import com.mediroute.service.ride.RideService;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class RideServiceTest {
//
//    @Mock
//    private RideRepository rideRepository;
//
//    @Mock
//    private PatientRepository patientRepository;
//
//    @InjectMocks
//    private RideService rideService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void testParseExcelFile() throws IOException {
//        // Create a minimal valid .xlsx file in-memory with the provided format
//        Workbook workbook = new XSSFWorkbook();
//        Sheet sheet = workbook.createSheet("Rides");
//        Row header = sheet.createRow(0);
//        header.createCell(0).setCellValue("NAME");
//        header.createCell(1).setCellValue("PHONE");
//        header.createCell(2).setCellValue("PICK UP");
//        header.createCell(3).setCellValue("DROP OFF");
//        header.createCell(4).setCellValue("Purpose");
//        header.createCell(5).setCellValue("TIME");
//        header.createCell(6).setCellValue("DISTANCE");
//        header.createCell(7).setCellValue("NOTE");
//        header.createCell(8).setCellValue("Run ID");
//        header.createCell(9).setCellValue("CANCELLED");
//        header.createCell(10).setCellValue("DRIVER");
//        header.createCell(11).setCellValue("RETURN");
//
//        // Row 1: Valid ride
//        Row row1 = sheet.createRow(1);
//        row1.createCell(0).setCellValue("ADANA ESTR");
//        row1.createCell(1).setCellValue("7204917760");
//        row1.createCell(2).setCellValue("4785 S Pagosa Way Aurora 80015");
//        row1.createCell(3).setCellValue("723 Delaware St Denver 80204");
//        row1.createCell(4).setCellValue("Dialysis");
//        row1.createCell(5).setCellValue("4:00");
//        row1.createCell(6).setCellValue("17.58");
//        row1.createCell(7).setCellValue("");
//        row1.createCell(8).setCellValue("ABU38");
//        row1.createCell(9).setCellValue("NO");
//        row1.createCell(10).setCellValue("");
//        row1.createCell(11).setCellValue("NO");
//
//        // Row 2: Canceled ride (skip)
//        Row row2 = sheet.createRow(2);
//        row2.createCell(0).setCellValue("JEATE LOY");
//        row2.createCell(1).setCellValue("3033590683");
//        row2.createCell(2).setCellValue("3002 Peoria St Aurora 80010");
//        row2.createCell(3).setCellValue("482 S Chambers Rd Aurora 80017");
//        row2.createCell(4).setCellValue("Dialysis");
//        row2.createCell(5).setCellValue("4:00");
//        row2.createCell(6).setCellValue("5.32");
//        row2.createCell(7).setCellValue("");
//        row2.createCell(8).setCellValue("ABU38");
//        row2.createCell(9).setCellValue("YES");
//        row2.createCell(10).setCellValue("");
//        row2.createCell(11).setCellValue("NO");
//
//        // Row 3: Return trip "YES"
//        Row row3 = sheet.createRow(3);
//        row3.createCell(0).setCellValue("CONLO MADEFIERROS");
//        row3.createCell(1).setCellValue("7202532077");
//        row3.createCell(2).setCellValue("5025 Scranton Ct Denver 80239");
//        row3.createCell(3).setCellValue("962 N Potomac Cir Aurora 80011");
//        row3.createCell(4).setCellValue("Dialysis");
//        row3.createCell(5).setCellValue("4:30");
//        row3.createCell(6).setCellValue("5.22");
//        row3.createCell(7).setCellValue("");
//        row3.createCell(8).setCellValue("ABU15");
//        row3.createCell(9).setCellValue("NO");
//        row3.createCell(10).setCellValue("");
//        row3.createCell(11).setCellValue("YES");
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        workbook.write(out);
//        byte[] excelContent = out.toByteArray();
//        workbook.close();
//
//        // Create MockMultipartFile
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.xlsx",
//                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
//                excelContent
//        );
//
//        // Mock patient repository lookup and save
//        when(patientRepository.findByNameAndContactInfo(anyString(), anyString())).thenReturn(Optional.empty());
//        when(patientRepository.save(any(Patient.class))).thenReturn(new Patient());
//
//        // Mock ride repository save
//        when(rideRepository.save(any(Ride.class))).thenReturn(new Ride());
//
//        // Test
//        List<Ride> result = rideService.parseExcelFile(file);
//
//        // Verify
//        assertEquals(3, result.size()); // 1 for row1, 1 for row3, 1 for return trip
//        verify(patientRepository, times(2)).save(any(Patient.class)); // For row1 and row3
//        verify(rideRepository, times(3)).save(any(Ride.class)); // 1 for row1, 1 for row3, 1 for return
//    }
//
//    @Test
//    void testOptimizeSchedule() {
//        Loader.loadNativeLibraries(); // Load native libraries
//
//        // Create test rides with feasible data
//        List<Ride> rides = new ArrayList<>();
//        Ride ride1 = new Ride();
//        ride1.setPickupTime(LocalDateTime.now().plusMinutes(10)); // Start soon
//        ride1.setWaitTime(5);
//        ride1.setDistance(1.0f); // Small distance
//        ride1.setStatus("scheduled");
//        rides.add(ride1);
//        Ride ride2 = new Ride();
//        ride2.setPickupTime(ride1.getPickupTime().plusMinutes(5)); // Close to ride1
//        ride2.setWaitTime(10);
//        ride2.setDistance(1.0f); // Small distance
//        ride2.setStatus("scheduled");
//        rides.add(ride2);
//
//        // Mock save
//        when(rideRepository.saveAll(any())).thenReturn(rides);
//
//        // Test
//        rideService.optimizeSchedule(rides);
//
//        // Verify
//        verify(rideRepository, times(1)).saveAll(any());
//        assertEquals(1L, ride1.getDriverId()); // Placeholder check
//        assertEquals(1L, ride2.getDriverId());
//    }
//
//    @Test
//    void testOptimizeScheduleSingleRide() {
//        Loader.loadNativeLibraries(); // Load native libraries
//
//        // Single ride (should always be feasible)
//        List<Ride> rides = new ArrayList<>();
//        Ride ride1 = new Ride();
//        ride1.setPickupTime(LocalDateTime.now().plusMinutes(10));
//        ride1.setWaitTime(5);
//        ride1.setDistance(1.0f);
//        ride1.setStatus("scheduled");
//        rides.add(ride1);
//
//        // Mock save
//        when(rideRepository.saveAll(any())).thenReturn(rides);
//
//        // Test
//        rideService.optimizeSchedule(rides);
//
//        // Verify
//        verify(rideRepository, times(1)).saveAll(any());
//        assertEquals(1L, ride1.getDriverId());
//    }
//
//
//}