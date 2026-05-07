package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.AlertEvaluateRequest;
import com.example.aiops.entity.OpsAlert;
import com.example.aiops.entity.OpsAlertRule;
import com.example.aiops.entity.OpsMetricPoint;
import com.example.aiops.mapper.OpsAlertMapper;
import com.example.aiops.mapper.OpsMetricPointMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private final OpsAlertMapper opsAlertMapper;
    private final AlertRuleService alertRuleService;
    private final OpsMetricPointMapper opsMetricPointMapper;

    public AlertService(OpsAlertMapper opsAlertMapper,
                        AlertRuleService alertRuleService,
                        OpsMetricPointMapper opsMetricPointMapper) {
        this.opsAlertMapper = opsAlertMapper;
        this.alertRuleService = alertRuleService;
        this.opsMetricPointMapper = opsMetricPointMapper;
    }

    public Map<String, Object> evaluate(AlertEvaluateRequest request) {
        OpsAlertRule rule = alertRuleService.resolveRule(request.getServiceName());

        boolean ruleTriggered = request.getErrorRate() >= rule.getErrorRateThreshold()
                || request.getLatencyMs() >= rule.getLatencyThresholdMs();

        boolean anomalyTriggered = isMetricAnomalyTriggered(request.getServiceName(), rule);
        boolean triggered = ruleTriggered || anomalyTriggered;

        Map<String, Object> result = new HashMap<>();
        result.put("triggered", triggered);
        result.put("ruleId", rule.getId());
        result.put("ruleTriggered", ruleTriggered);
        result.put("anomalyTriggered", anomalyTriggered);

        if (!triggered) {
            result.put("message", "rule not triggered");
            return result;
        }

        if (isSuppressed(request.getServiceName(), rule.getSuppressWindowSec())) {
            result.put("suppressed", true);
            result.put("message", "alert suppressed by window");
            return result;
        }

        String dedupKey = request.getServiceName() + "|" + normalizeSummary(request.getSummary());
        OpsAlert duplicated = findDuplicatedRecentAlert(request.getServiceName(), dedupKey, rule.getDedupWindowSec());
        if (duplicated != null) {
            result.put("deduplicated", true);
            result.put("message", "alert deduplicated");
            result.put("alertId", duplicated.getId());
            result.put("severity", duplicated.getSeverity());
            return result;
        }

        String severity = request.getErrorRate() >= (rule.getErrorRateThreshold() * 2)
                || request.getLatencyMs() >= (rule.getLatencyThresholdMs() * 2) ? "P1" : "P2";

        OpsAlert alert = new OpsAlert();
        alert.setServiceName(request.getServiceName());
        alert.setSeverity(severity);
        alert.setStatus("NEW");
        alert.setTitle("Auto alert for " + request.getServiceName());
        alert.setDetail(dedupKey);
        alert.setRuleId(rule.getId());
        opsAlertMapper.insert(alert);

        result.put("suppressed", false);
        result.put("deduplicated", false);
        result.put("message", "alert triggered");
        result.put("alertId", alert.getId());
        result.put("severity", severity);
        return result;
    }

    private boolean isSuppressed(String serviceName, Long suppressWindowSec) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(suppressWindowSec);
        Long count = opsAlertMapper.selectCount(new LambdaQueryWrapper<OpsAlert>()
                .eq(OpsAlert::getServiceName, serviceName)
                .in(OpsAlert::getStatus, "NEW", "ACK", "IN_PROGRESS")
                .ge(OpsAlert::getCreatedAt, threshold));
        return count != null && count > 0;
    }

    private OpsAlert findDuplicatedRecentAlert(String serviceName, String dedupKey, Long dedupWindowSec) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(dedupWindowSec);
        return opsAlertMapper.selectOne(new LambdaQueryWrapper<OpsAlert>()
                .eq(OpsAlert::getServiceName, serviceName)
                .eq(OpsAlert::getDetail, dedupKey)
                .ge(OpsAlert::getCreatedAt, threshold)
                .orderByDesc(OpsAlert::getId)
                .last("limit 1"));
    }

    public List<OpsAlert> list(String serviceName, String status) {
        LambdaQueryWrapper<OpsAlert> wrapper = new LambdaQueryWrapper<>();
        if (serviceName != null && !serviceName.isBlank()) {
            wrapper.eq(OpsAlert::getServiceName, serviceName);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(OpsAlert::getStatus, status);
        }
        wrapper.orderByDesc(OpsAlert::getId);
        return opsAlertMapper.selectList(wrapper);
    }

    private boolean isMetricAnomalyTriggered(String serviceName, OpsAlertRule rule) {
        long now = System.currentTimeMillis();
        long recentStart = now - 5 * 60_000;
        long baselineStart = now - 30 * 60_000;
        long baselineEnd = now - 5 * 60_000;

        List<OpsMetricPoint> recentLatencyPoints = opsMetricPointMapper.selectList(new LambdaQueryWrapper<OpsMetricPoint>()
                .eq(OpsMetricPoint::getServiceName, serviceName)
                .eq(OpsMetricPoint::getMetricType, "latency_ms")
                .ge(OpsMetricPoint::getProbeTime, recentStart)
                .orderByDesc(OpsMetricPoint::getProbeTime)
                .last("limit 20"));

        if (recentLatencyPoints.isEmpty()) {
            return false;
        }

        double recentAvg = recentLatencyPoints.stream()
                .mapToDouble(OpsMetricPoint::getMetricValue)
                .average()
                .orElse(0D);

        List<OpsMetricPoint> baselineLatencyPoints = opsMetricPointMapper.selectList(new LambdaQueryWrapper<OpsMetricPoint>()
                .eq(OpsMetricPoint::getServiceName, serviceName)
                .eq(OpsMetricPoint::getMetricType, "latency_ms")
                .ge(OpsMetricPoint::getProbeTime, baselineStart)
                .lt(OpsMetricPoint::getProbeTime, baselineEnd)
                .orderByDesc(OpsMetricPoint::getProbeTime)
                .last("limit 100"));

        double baselineAvg = baselineLatencyPoints.stream()
                .mapToDouble(OpsMetricPoint::getMetricValue)
                .average()
                .orElse((double) rule.getLatencyThresholdMs());

        if (recentAvg >= baselineAvg * 2 && recentAvg >= rule.getLatencyThresholdMs()) {
            return true;
        }

        long recentErrorCount = opsMetricPointMapper.selectCount(new LambdaQueryWrapper<OpsMetricPoint>()
                .eq(OpsMetricPoint::getServiceName, serviceName)
                .eq(OpsMetricPoint::getMetricType, "probe_error")
                .ge(OpsMetricPoint::getProbeTime, recentStart));

        return recentErrorCount >= 3;
    }

    private String normalizeSummary(String summary) {
        if (summary == null) {
            return "unknown";
        }
        return summary.trim().toLowerCase();
    }
}
