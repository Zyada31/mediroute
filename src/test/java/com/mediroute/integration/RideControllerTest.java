package com.mediroute.integration;

import com.mediroute.repository.PatientRepository;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.RideService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RideService rideService;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        rideRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    void testUploadExcel() throws Exception {
        // Create a minimal Excel file with the provided format (same as unit test)
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rides");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("NAME");
        header.createCell(1).setCellValue("PHONE");
        header.createCell(2).setCellValue("PICK UP");
        header.createCell(3).setCellValue("DROP OFF");
        header.createCell(4).setCellValue("Purpose");
        header.createCell(5).setCellValue("TIME");
        header.createCell(6).setCellValue("DISTANCE");
        header.createCell(7).setCellValue("NOTE");
        header.createCell(8).setCellValue("Run ID");
        header.createCell(9).setCellValue("CANCELLED");
        header.createCell(10).setCellValue("DRIVER");
        header.createCell(11).setCellValue("RETURN");

        // Add sample rows (same as unit test)
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("ADANA ESTR");
        row1.createCell(1).setCellValue("7204917760");
        row1.createCell(2).setCellValue("4785 S Pagosa Way Aurora 80015");
        row1.createCell(3).setCellValue("723 Delaware St Denver 80204");
        row1.createCell(4).setCellValue("Dialysis");
        row1.createCell(5).setCellValue("4:00");
        row1.createCell(6).setCellValue("17.58");
        row1.createCell(7).setCellValue("");
        row1.createCell(8).setCellValue("ABU38");
        row1.createCell(9).setCellValue("NO");
        row1.createCell(10).setCellValue("");
        row1.createCell(11).setCellValue("NO");

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("JEATE LOY");
        row2.createCell(1).setCellValue("3033590683");
        row2.createCell(2).setCellValue("3002 Peoria St Aurora 80010");
        row2.createCell(3).setCellValue("482 S Chambers Rd Aurora 80017");
        row2.createCell(4).setCellValue("Dialysis");
        row2.createCell(5).setCellValue("4:00");
        row2.createCell(6).setCellValue("5.32");
        row2.createCell(7).setCellValue("");
        row2.createCell(8).setCellValue("ABU38");
        row2.createCell(9).setCellValue("YES");
        row2.createCell(10).setCellValue("");
        row2.createCell(11).setCellValue("NO");

        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("CONLO MADEFIERROS");
        row3.createCell(1).setCellValue("7202532077");
        row3.createCell(2).setCellValue("5025 Scranton Ct Denver 80239");
        row3.createCell(3).setCellValue("962 N Potomac Cir Aurora 80011");
        row3.createCell(4).setCellValue("Dialysis");
        row3.createCell(5).setCellValue("4:30");
        row3.createCell(6).setCellValue("5.22");
        row3.createCell(7).setCellValue("");
        row3.createCell(8).setCellValue("ABU15");
        row3.createCell(9).setCellValue("NO");
        row3.createCell(10).setCellValue("");
        row3.createCell(11).setCellValue("YES");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        byte[] excelContent = out.toByteArray();
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // Perform the upload
        mockMvc.perform(multipart("/api/rides/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        // Verify 2 patients and 3 rides were saved (row1, row3, return for row3)
        assertEquals(2, patientRepository.count());
        assertEquals(3, rideRepository.count());
    }
}