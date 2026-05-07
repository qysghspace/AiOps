package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.MonitorTarget;
import com.example.aiops.entity.OpsMetricPoint;
import com.example.aiops.mapper.MonitorTargetMapper;
import com.example.aiops.mapper.OpsMetricPointMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "aiops.monitor", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MonitorProbeService {

    private final MonitorTargetMapper monitorTargetMapper;
    private final OpsMetricPointMapper opsMetricPointMapper;
    private final AlertService alertService;
    private final RestClient restClient;

    public MonitorProbeService(MonitorTargetMapper monitorTargetMapper,
                               OpsMetricPointMapper opsMetricPointMapper,
                               AlertService alertService,
                               RestClient.Builder restClientBuilder) {
        this.monitorTargetMapper = monitorTargetMapper;
        this.opsMetricPointMapper = opsMetricPointMapper;
        this.alertService = alertService;
        this.restClient = restClientBuilder.build();
    }

    @Scheduled(fixedDelayString = "${aiops.monitor.probe-fixed-delay-ms:30000}")
    public void probeTargets() {
        List<MonitorTarget> targets = monitorTargetMapper.selectList(new LambdaQueryWrapper<MonitorTarget>()
                .eq(MonitorTarget::getEnabled, "Y"));

        for (MonitorTarget target : targets) {
            probeSingleTarget(target);
        }
    }

    private void probeSingleTarget(MonitorTarget target) {
        long start = System.currentTimeMillis();
        long probeTime = start;
        String status = "DOWN";
        String detail;
        long latency;

        try {
            String protocol = target.getProtocol() == null ? "HTTP" : target.getProtocol().toUpperCase();
            if ("TCP".equals(protocol)) {
                ProbeResult r = probeTcp(target);
                latency = r.latency;
                status = r.up ? "UP" : "DOWN";
                detail = r.detail;
                savePoint(target, "latency_ms", (double) latency, status, probeTime, detail);
                savePoint(target, "tcp_connect", r.up ? 1D : 0D, status, probeTime, detail);
            } else {
                int statusCode = restClient.get()
                        .uri(target.getTargetUrl())
                        .retrieve()
                        .toBodilessEntity()
                        .getStatusCode()
                        .value();

                latency = System.currentTimeMillis() - start;
                boolean statusMatch = matchExpectedStatus(statusCode, target.getExpectedStatus());
                status = statusMatch ? "UP" : "DOWN";
                detail = "httpStatus=" + statusCode + ", expected=" + target.getExpectedStatus();

                savePoint(target, "latency_ms", (double) latency, status, probeTime, detail);
                savePoint(target, "http_status", (double) statusCode, status, probeTime, detail);
            }

            if ("DOWN".equals(status)) {
                triggerAlertFromProbe(target, detail, latency = System.currentTimeMillis() - start);
            }
        } catch (Exception ex) {
            latency = System.currentTimeMillis() - start;
            detail = ex.getClass().getSimpleName() + ": " + safeMsg(ex.getMessage());
            savePoint(target, "latency_ms", (double) latency, status, probeTime, detail);
            savePoint(target, "probe_error", 1D, status, probeTime, detail);
            triggerAlertFromProbe(target, detail, latency);
        }
    }

    private boolean matchExpectedStatus(int statusCode, String expectedStatus) {
        if (expectedStatus == null || expectedStatus.isBlank() || "2xx".equalsIgnoreCase(expectedStatus)) {
            return statusCode >= 200 && statusCode < 300;
        }
        if ("3xx".equalsIgnoreCase(expectedStatus)) {
            return statusCode >= 300 && statusCode < 400;
        }
        if ("4xx".equalsIgnoreCase(expectedStatus)) {
            return statusCode >= 400 && statusCode < 500;
        }
        if ("5xx".equalsIgnoreCase(expectedStatus)) {
            return statusCode >= 500 && statusCode < 600;
        }
        try {
            return statusCode == Integer.parseInt(expectedStatus);
        } catch (NumberFormatException ignore) {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    private void savePoint(MonitorTarget target,
                           String metricType,
                           Double metricValue,
                           String status,
                           long probeTime,
                           String detail) {
        OpsMetricPoint point = new OpsMetricPoint();
        point.setServiceName(target.getServiceName());
        point.setTargetId(target.getId());
        point.setMetricType(metricType);
        point.setMetricValue(metricValue);
        point.setStatus(status);
        point.setProbeTime(probeTime);
        point.setDetail(detail);
        opsMetricPointMapper.insert(point);
    }

    private ProbeResult probeTcp(MonitorTarget target) {
        long start = System.currentTimeMillis();
        String host = target.getTargetHost();
        Integer port = target.getTargetPort();
        int timeout = target.getTimeoutMs() == null ? 3000 : target.getTimeoutMs();
        if (host == null || host.isBlank() || port == null) {
            return new ProbeResult(false, 0L, "tcp target missing host/port");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(true, latency, "tcp connect ok");
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, latency, ex.getClass().getSimpleName() + ": " + safeMsg(ex.getMessage()));
        }
    }

    private void triggerAlertFromProbe(MonitorTarget target, String detail, long latency) {
        com.example.aiops.entity.AlertEvaluateRequest req = new com.example.aiops.entity.AlertEvaluateRequest();
        req.setServiceName(target.getServiceName());
        req.setErrorRate(100D);
        req.setLatencyMs(latency);
        req.setSummary("probe down: " + detail);
        alertService.evaluate(req);
    }

    private record ProbeResult(boolean up, long latency, String detail) {}

    private String safeMsg(String msg) {
        if (msg == null || msg.isBlank()) {
            return "no message";
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
