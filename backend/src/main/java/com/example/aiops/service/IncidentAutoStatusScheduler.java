package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.IncidentStatusUpdateRequest;
import com.example.aiops.entity.OpsAlert;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.mapper.OpsAlertMapper;
import com.example.aiops.mapper.OpsIncidentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class IncidentAutoStatusScheduler {

    private final OpsIncidentMapper opsIncidentMapper;
    private final OpsAlertMapper opsAlertMapper;
    private final IncidentService incidentService;

    @Value("${aiops.incident.auto.enabled:true}")
    private boolean enabled;

    @Value("${aiops.incident.auto.resolve-after-minutes:5}")
    private int resolveAfterMinutes;

    @Value("${aiops.incident.auto.close-after-minutes:30}")
    private int closeAfterMinutes;

    public IncidentAutoStatusScheduler(OpsIncidentMapper opsIncidentMapper,
                                       OpsAlertMapper opsAlertMapper,
                                       IncidentService incidentService) {
        this.opsIncidentMapper = opsIncidentMapper;
        this.opsAlertMapper = opsAlertMapper;
        this.incidentService = incidentService;
    }

    @Scheduled(fixedDelayString = "${aiops.incident.auto.scan-interval-ms:60000}")
    public void autoTransit() {
        if (!enabled) return;
        List<OpsIncident> incidents = opsIncidentMapper.selectList(new LambdaQueryWrapper<OpsIncident>()
                .in(OpsIncident::getStatus, List.of("OPEN", "IN_PROGRESS", "RESOLVED"))
                .orderByAsc(OpsIncident::getId)
                .last("limit 500"));
        for (OpsIncident inc : incidents) {
            tryTransit(inc);
        }
    }

    private void tryTransit(OpsIncident inc) {
        if (inc.getAlertId() == null) return;
        OpsAlert alert = opsAlertMapper.selectById(inc.getAlertId());
        if (alert == null) return;
        String aStatus = alert.getStatus() == null ? "OPEN" : alert.getStatus().toUpperCase();

        if ("OPEN".equals(inc.getStatus())) {
            if ("OPEN".equals(aStatus) || "IN_PROGRESS".equals(aStatus)) {
                change(inc.getId(), "IN_PROGRESS");
            }
            return;
        }

        if ("IN_PROGRESS".equals(inc.getStatus())) {
            if (isRecoveredAlert(aStatus) && minutesSince(inc.getUpdatedAt()) >= resolveAfterMinutes) {
                change(inc.getId(), "RESOLVED");
            }
            return;
        }

        if ("RESOLVED".equals(inc.getStatus())) {
            if (!isRecoveredAlert(aStatus)) {
                change(inc.getId(), "IN_PROGRESS");
                return;
            }
            if (minutesSince(inc.getUpdatedAt()) >= closeAfterMinutes) {
                change(inc.getId(), "CLOSED");
            }
        }
    }

    private void change(Long id, String toStatus) {
        IncidentStatusUpdateRequest req = new IncidentStatusUpdateRequest();
        req.setStatus(toStatus);
        incidentService.autoUpdateStatus(id, req);
    }

    private boolean isRecoveredAlert(String alertStatus) {
        return "RESOLVED".equals(alertStatus) || "CLOSED".equals(alertStatus);
    }

    private long minutesSince(LocalDateTime t) {
        if (t == null) return Long.MAX_VALUE;
        return java.time.Duration.between(t, LocalDateTime.now()).toMinutes();
    }
}
