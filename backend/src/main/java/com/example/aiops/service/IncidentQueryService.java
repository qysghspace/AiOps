package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.OpsAiAnalysis;
import com.example.aiops.entity.OpsIncidentTimeline;
import com.example.aiops.mapper.MonitorTargetMapper;
import com.example.aiops.mapper.OpsAiAnalysisMapper;
import com.example.aiops.mapper.OpsAlertMapper;
import com.example.aiops.mapper.OpsIncidentMapper;
import com.example.aiops.mapper.OpsIncidentTimelineMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentQueryService {

    private final OpsIncidentTimelineMapper opsIncidentTimelineMapper;
    private final OpsAiAnalysisMapper opsAiAnalysisMapper;
    private final OpsIncidentMapper opsIncidentMapper;
    private final OpsAlertMapper opsAlertMapper;
    private final MonitorTargetMapper monitorTargetMapper;

    public IncidentQueryService(OpsIncidentTimelineMapper opsIncidentTimelineMapper,
                                OpsAiAnalysisMapper opsAiAnalysisMapper,
                                OpsIncidentMapper opsIncidentMapper,
                                OpsAlertMapper opsAlertMapper,
                                MonitorTargetMapper monitorTargetMapper) {
        this.opsIncidentTimelineMapper = opsIncidentTimelineMapper;
        this.opsAiAnalysisMapper = opsAiAnalysisMapper;
        this.opsIncidentMapper = opsIncidentMapper;
        this.opsAlertMapper = opsAlertMapper;
        this.monitorTargetMapper = monitorTargetMapper;
    }

    public Map<String, Object> detail(Long incidentId) {
        List<OpsIncidentTimeline> timeline = opsIncidentTimelineMapper.selectList(new LambdaQueryWrapper<OpsIncidentTimeline>()
                .eq(OpsIncidentTimeline::getIncidentId, incidentId)
                .orderByAsc(OpsIncidentTimeline::getId));

        List<OpsAiAnalysis> analysis = opsAiAnalysisMapper.selectList(new LambdaQueryWrapper<OpsAiAnalysis>()
                .eq(OpsAiAnalysis::getIncidentId, incidentId)
                .orderByDesc(OpsAiAnalysis::getId));

        Map<String, Object> result = new HashMap<>();
        result.put("timeline", timeline);
        result.put("analysis", analysis);
        return result;
    }

    public List<OpsAiAnalysis> analysisHistory() {
        return opsAiAnalysisMapper.selectList(new LambdaQueryWrapper<OpsAiAnalysis>()
                .orderByDesc(OpsAiAnalysis::getId)
                .last("limit 200"));
    }

    public Map<String, Object> dashboardStats() {
        long activeTargets = monitorTargetMapper.selectCount(new LambdaQueryWrapper<com.example.aiops.entity.MonitorTarget>()
                .eq(com.example.aiops.entity.MonitorTarget::getEnabled, "Y"));

        List<com.example.aiops.entity.OpsAlert> allAlerts = opsAlertMapper.selectList(new LambdaQueryWrapper<com.example.aiops.entity.OpsAlert>()
                .orderByDesc(com.example.aiops.entity.OpsAlert::getId)
                .last("limit 2000"));

        LocalDate today = LocalDate.now();
        int todayAlertCount = 0;
        int highSeverityOpenCount = 0;
        for (com.example.aiops.entity.OpsAlert a : allAlerts) {
            if (a.getCreatedAt() != null && today.equals(a.getCreatedAt().toLocalDate())) {
                todayAlertCount++;
            }
            String severity = a.getSeverity() == null ? "" : a.getSeverity().toUpperCase();
            String status = a.getStatus() == null ? "" : a.getStatus().toUpperCase();
            boolean highSeverity = severity.equals("P1") || severity.equals("P2") || severity.equals("HIGH") || severity.equals("CRITICAL");
            boolean openLike = !status.equals("RESOLVED") && !status.equals("CLOSED");
            if (highSeverity && openLike) {
                highSeverityOpenCount++;
            }
        }

        List<com.example.aiops.entity.OpsIncident> allIncidents = opsIncidentMapper.selectList(new LambdaQueryWrapper<com.example.aiops.entity.OpsIncident>()
                .orderByDesc(com.example.aiops.entity.OpsIncident::getId)
                .last("limit 2000"));
        int resolvedCount = 0;
        for (com.example.aiops.entity.OpsIncident i : allIncidents) {
            String status = i.getStatus() == null ? "" : i.getStatus().toUpperCase();
            if (status.equals("RESOLVED") || status.equals("CLOSED")) {
                resolvedCount++;
            }
        }

        long aiSessionCount = opsAiAnalysisMapper.selectCount(new LambdaQueryWrapper<OpsAiAnalysis>());
        int healthRate = Math.max(20, 100 - Math.min(80, highSeverityOpenCount * 10));

        Map<String, Object> result = new HashMap<>();
        result.put("activeTargets", activeTargets);
        result.put("todayAlertCount", todayAlertCount);
        result.put("aiSessionCount", aiSessionCount);
        result.put("resolvedCount", resolvedCount);
        result.put("healthRate", healthRate);
        return result;
    }
}
