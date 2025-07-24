package com.mediroute.service;

import com.mediroute.entity.Ride;
import com.mediroute.parser.ExcelRideParser;
import com.mediroute.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final ExcelRideParser excelRideParser;
    private final RideOptimizerService rideOptimizerService;

    public List<Ride> parseExcelFile(MultipartFile file, LocalDate assignmentDate) throws IOException {
        List<Ride> rides = excelRideParser.parseExcel(file, assignmentDate);
        log.info("✅ Parsed and timestamp-adjusted {} rides for assignmentDate={}", rides.size(), assignmentDate);
        return rides;
    }

    public void optimizeSchedule(List<Ride> rides) {
        rideOptimizerService.optimize(rides);
    }
}