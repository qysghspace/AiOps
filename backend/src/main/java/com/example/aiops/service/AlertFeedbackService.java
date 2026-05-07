package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.AlertFeedbackSubmitRequest;
import com.example.aiops.entity.OpsAlert;
import com.example.aiops.entity.OpsAlertAnalysisFeedback;
import com.example.aiops.mapper.OpsAlertAnalysisFeedbackMapper;
import com.example.aiops.mapper.OpsAlertMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertFeedbackService {

    private static final List<String> REASON_DICT = List.of(
            "阈值配置不合理",
            "依赖服务异常",
            "网络抖动或中断",
            "资源不足",
            "发布/配置变更引发",
            "监控误报",
            "第三方接口异常",
            "数据库性能问题"
    );

    private final OpsAlertAnalysisFeedbackMapper feedbackMapper;
    private final OpsAlertMapper alertMapper;

    public AlertFeedbackService(OpsAlertAnalysisFeedbackMapper feedbackMapper,
                                OpsAlertMapper alertMapper) {
        this.feedbackMapper = feedbackMapper;
        this.alertMapper = alertMapper;
    }

    public Map<String, Object> submit(AlertFeedbackSubmitRequest request, String userName) {
        OpsAlertAnalysisFeedback row = new OpsAlertAnalysisFeedback();
        row.setAlertId(request.getAlertId());
        row.setIncidentId(request.getIncidentId());
        row.setAiAnalysisId(request.getAiAnalysisId());
        row.setSelectedReasons(toJsonArray(request.getSelectedReasons()));
        row.setReasonText(trimToNull(request.getReasonText()));
        row.setFalsePositive(Boolean.TRUE.equals(request.getFalsePositive()));
        row.setCreatedBy((userName == null || userName.isBlank()) ? "anonymous" : userName);
        row.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(row);

        Map<String, Object> data = new HashMap<>();
        data.put("id", row.getId());
        data.put("saved", true);
        return data;
    }

    public List<Map<String, Object>> listByAlertId(Long alertId) {
        return feedbackMapper.selectList(new LambdaQueryWrapper<OpsAlertAnalysisFeedback>()
                        .eq(OpsAlertAnalysisFeedback::getAlertId, alertId)
                        .orderByDesc(OpsAlertAnalysisFeedback::getId)
                        .last("limit 100"))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> similarCauses(Long alertId) {
        OpsAlert alert = alertMapper.selectById(alertId);
        if (alert == null) return List.of();

        List<OpsAlert> candidateAlerts = alertMapper.selectList(new LambdaQueryWrapper<OpsAlert>()
                .eq(OpsAlert::getServiceName, alert.getServiceName())
                .eq(OpsAlert::getSeverity, alert.getSeverity())
                .orderByDesc(OpsAlert::getId)
                .last("limit 200"));

        if (candidateAlerts.isEmpty()) return List.of();
        List<Long> ids = candidateAlerts.stream().map(OpsAlert::getId).toList();

        List<OpsAlertAnalysisFeedback> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<OpsAlertAnalysisFeedback>()
                .in(OpsAlertAnalysisFeedback::getAlertId, ids)
                .orderByDesc(OpsAlertAnalysisFeedback::getId)
                .last("limit 500"));

        Map<String, CauseAgg> agg = new LinkedHashMap<>();
        for (OpsAlertAnalysisFeedback f : feedbacks) {
            List<String> reasons = parseJsonArray(f.getSelectedReasons());
            for (String reason : reasons) {
                if (reason == null || reason.isBlank()) continue;
                CauseAgg item = agg.computeIfAbsent(reason, k -> new CauseAgg());
                item.reason = reason;
                item.hitCount++;
                item.latestAt = maxTime(item.latestAt, f.getCreatedAt());
            }
        }

        return agg.values().stream()
                .sorted(Comparator.comparingInt((CauseAgg c) -> c.hitCount).reversed()
                        .thenComparing((CauseAgg c) -> c.latestAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("reason", c.reason);
                    m.put("hitCount", c.hitCount);
                    m.put("latestAt", c.latestAt == null ? null : c.latestAt.toString());
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<String> reasonDict() {
        return REASON_DICT;
    }

    private Map<String, Object> toView(OpsAlertAnalysisFeedback x) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", x.getId());
        m.put("alertId", x.getAlertId());
        m.put("incidentId", x.getIncidentId());
        m.put("aiAnalysisId", x.getAiAnalysisId());
        m.put("selectedReasons", parseJsonArray(x.getSelectedReasons()));
        m.put("reasonText", x.getReasonText());
        m.put("falsePositive", x.getFalsePositive());
        m.put("createdBy", x.getCreatedBy());
        m.put("createdAt", x.getCreatedAt() == null ? null : x.getCreatedAt().toString());
        return m;
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        return "[" + list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return List.of();
        String t = json.trim();
        if (t.startsWith("[") && t.endsWith("]")) {
            t = t.substring(1, t.length() - 1);
        }
        if (t.isBlank()) return List.of();
        return Arrays.stream(t.split(","))
                .map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", ""))
                .map(s -> s.replace("\\\"", "\""))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private LocalDateTime maxTime(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static class CauseAgg {
        String reason;
        int hitCount;
        LocalDateTime latestAt;
    }
}
