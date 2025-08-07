package com.mediroute.service.ride;

import com.mediroute.entity.Ride;
import com.mediroute.entity.RideAudit;
import com.mediroute.repository.RideAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideAuditService {

    private final RideAuditRepository rideAuditRepository;

    public void auditChange(Ride ride, String field, Object oldVal, Object newVal, String changedBy) {
        if (Objects.equals(String.valueOf(oldVal), String.valueOf(newVal))) {
            return; // No real change
        }

        RideAudit audit = RideAudit.builder()
                .ride(ride)
                .fieldName(field) // ✅ match entity field name
                .oldValue(String.valueOf(oldVal))
                .newValue(String.valueOf(newVal))
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();

        rideAuditRepository.save(audit);
        log.info("📝 Audited change on Ride {}: {} {} → {}", ride.getId(), field, oldVal, newVal);
    }
}