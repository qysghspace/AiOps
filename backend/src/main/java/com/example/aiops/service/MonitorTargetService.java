package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.MonitorTarget;
import com.example.aiops.entity.AlertEvaluateRequest;
import com.example.aiops.entity.OpsAiAnalysis;
import com.example.aiops.entity.OpsAlert;
import com.example.aiops.entity.OpsMetricPoint;
import com.example.aiops.mapper.MonitorTargetMapper;
import com.example.aiops.mapper.OpsAiAnalysisMapper;
import com.example.aiops.mapper.OpsAlertMapper;
import com.example.aiops.mapper.OpsMetricPointMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitorTargetService {

    private final MonitorTargetMapper monitorTargetMapper;
    private final OpsMetricPointMapper opsMetricPointMapper;
    private final OpsAlertMapper opsAlertMapper;
    private final OpsAiAnalysisMapper opsAiAnalysisMapper;
    private final AlertService alertService;

    public MonitorTargetService(MonitorTargetMapper monitorTargetMapper,
                                OpsMetricPointMapper opsMetricPointMapper,
                                OpsAlertMapper opsAlertMapper,
                                OpsAiAnalysisMapper opsAiAnalysisMapper,
                                AlertService alertService) {
        this.monitorTargetMapper = monitorTargetMapper;
        this.opsMetricPointMapper = opsMetricPointMapper;
        this.opsAlertMapper = opsAlertMapper;
        this.opsAiAnalysisMapper = opsAiAnalysisMapper;
        this.alertService = alertService;
    }

    public List<MonitorTarget> list() {
        return monitorTargetMapper.selectList(new LambdaQueryWrapper<MonitorTarget>().orderByDesc(MonitorTarget::getId));
    }

    public MonitorTarget create(Map<String, Object> body) {
        MonitorTarget t = new MonitorTarget();
        String serviceName = String.valueOf(body.getOrDefault("name", "")).trim();
        String host = String.valueOf(body.getOrDefault("ip", "127.0.0.1")).trim();
        int port = toInt(body.get("port"), 80);

        t.setServiceName(serviceName.isEmpty() ? "default-target" : serviceName);
        t.setTargetHost(host.isEmpty() ? "127.0.0.1" : host);
        t.setTargetPort(port);
        t.setTargetUrl("http://" + t.getTargetHost() + ":" + port);
        t.setIntervalSec(toInt(body.get("intervalSeconds"), 30));
        t.setProtocol("TCP");
        t.setExpectedStatus("2xx");
        t.setTimeoutMs(3000);
        t.setEnabled("Y");
        t.setCreatedAt(LocalDateTime.now());
        monitorTargetMapper.insert(t);
        return t;
    }

    public MonitorTarget update(Long id, Map<String, Object> body) {
        MonitorTarget t = monitorTargetMapper.selectById(id);
        if (t == null) return null;
        t.setServiceName(String.valueOf(body.getOrDefault("name", t.getServiceName())));
        t.setTargetHost(String.valueOf(body.getOrDefault("ip", t.getTargetHost())));
        t.setTargetPort(toInt(body.get("port"), t.getTargetPort() == null ? 80 : t.getTargetPort()));
        t.setTargetUrl("http://" + t.getTargetHost() + ":" + t.getTargetPort());
        t.setIntervalSec(toInt(body.get("intervalSeconds"), t.getIntervalSec() == null ? 30 : t.getIntervalSec()));
        if (t.getExpectedStatus() == null) t.setExpectedStatus("2xx");
        if (t.getTimeoutMs() == null) t.setTimeoutMs(3000);
        if (t.getProtocol() == null) t.setProtocol("TCP");
        if (t.getEnabled() == null) t.setEnabled("Y");
        monitorTargetMapper.updateById(t);
        return t;
    }

    public boolean delete(Long id) {
        return monitorTargetMapper.deleteById(id) > 0;
    }

    public MonitorTarget stopProbe(Long id) {
        MonitorTarget t = monitorTargetMapper.selectById(id);
        if (t == null) return null;
        t.setEnabled("N");
        monitorTargetMapper.updateById(t);
        return t;
    }

    public MonitorTarget resumeProbe(Long id) {
        MonitorTarget t = monitorTargetMapper.selectById(id);
        if (t == null) return null;
        t.setEnabled("Y");
        monitorTargetMapper.updateById(t);
        return t;
    }

    public Map<String, Object> probe(Long id) {
        MonitorTarget t = monitorTargetMapper.selectById(id);
        if (t == null) {
            return Map.of("targetId", id, "triggered", false, "message", "target not found");
        }

        boolean up = isTcpReachable(t.getTargetHost(), t.getTargetPort(), t.getTimeoutMs() == null ? 3000 : t.getTimeoutMs());
        long now = System.currentTimeMillis();

        OpsMetricPoint latencyPoint = new OpsMetricPoint();
        latencyPoint.setServiceName(t.getServiceName());
        latencyPoint.setTargetId(t.getId());
        latencyPoint.setMetricType("latency_ms");
        latencyPoint.setMetricValue(up ? 20D : 9999D);
        latencyPoint.setStatus(up ? "UP" : "DOWN");
        latencyPoint.setProbeTime(now);
        latencyPoint.setDetail(up ? "manual probe ok" : "manual probe down");
        opsMetricPointMapper.insert(latencyPoint);

        if (!up) {
            AlertEvaluateRequest req = new AlertEvaluateRequest();
            req.setServiceName(t.getServiceName());
            req.setErrorRate(100D);
            req.setLatencyMs(9999L);
            req.setSummary("manual probe down " + t.getTargetHost() + ":" + t.getTargetPort());
            Map<String, Object> eval = alertService.evaluate(req);
            return Map.of("targetId", id, "triggered", true, "probe", "DOWN", "alert", eval);
        }

        return Map.of("targetId", id, "triggered", true, "probe", "UP");
    }

    private boolean isTcpReachable(String host, Integer port, int timeoutMs) {
        if (host == null || host.isBlank() || port == null) return false;
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Map<String, Object>> metrics(Long id) {
        List<OpsMetricPoint> points = opsMetricPointMapper.selectList(new LambdaQueryWrapper<OpsMetricPoint>()
                .eq(OpsMetricPoint::getTargetId, id)
                .orderByDesc(OpsMetricPoint::getId)
                .last("limit 100"));
        return points.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("time", p.getProbeTime());
            m.put("latency", p.getMetricValue());
            m.put("reachable", "UP".equalsIgnoreCase(p.getStatus()) ? "UP" : "DOWN");
            return m;
        }).toList();
    }

    public List<Map<String, Object>> alerts(Long id) {
        MonitorTarget t = monitorTargetMapper.selectById(id);
        if (t == null) return List.of();
        List<OpsAlert> alerts = opsAlertMapper.selectList(new LambdaQueryWrapper<OpsAlert>()
                .eq(OpsAlert::getServiceName, t.getServiceName())
                .orderByDesc(OpsAlert::getId)
                .last("limit 50"));
        return alerts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("content", (a.getTitle() == null ? "" : a.getTitle()) + (a.getDetail() == null ? "" : (" | " + a.getDetail())));
            m.put("severity", a.getSeverity());
            m.put("time", a.getCreatedAt());
            return m;
        }).toList();
    }

    public List<Map<String, Object>> analyses(Long id) {
        List<OpsAiAnalysis> list = opsAiAnalysisMapper.selectList(new LambdaQueryWrapper<OpsAiAnalysis>()
                .orderByDesc(OpsAiAnalysis::getId)
                .last("limit 50"));
        return list.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("sessionId", "analysis-" + a.getId());
            m.put("summary", a.getSuggestion());
            m.put("createdAt", a.getId());
            return m;
        }).toList();
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
}
