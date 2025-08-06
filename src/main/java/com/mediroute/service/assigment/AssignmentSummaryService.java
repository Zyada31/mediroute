package com.mediroute.service.assigment;

import com.mediroute.dto.DriverRideSummary;
import com.mediroute.entity.Ride;
import com.mediroute.repository.RideRepository;
import com.mediroute.service.parser.EnhancedExcelRideParser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentSummaryService {
    Logger log = LoggerFactory.getLogger(AssignmentSummaryService.class);

    private final RideRepository rideRepository;

    public List<DriverRideSummary> getSummaryForDate(LocalDateTime dayStart, LocalDateTime dayEnd) {
        List<Ride> assignedRides = rideRepository.findByPickupTimeBetweenAndDriverIsNotNull(dayStart, dayEnd);

        return assignedRides.stream()
                .collect(Collectors.groupingBy(
                        Ride::getDriver,
                        Collectors.mapping(Ride::getPickupTime, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .map(entry -> new DriverRideSummary(
                        entry.getKey().getId(),
                        entry.getKey().getName(),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList();
    }
}