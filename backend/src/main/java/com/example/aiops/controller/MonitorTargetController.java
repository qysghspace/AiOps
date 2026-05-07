package com.example.aiops.controller;

import com.example.aiops.common.ApiResponse;
import com.example.aiops.entity.MonitorTarget;
import com.example.aiops.service.MonitorTargetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor-targets")
public class MonitorTargetController {

    private final MonitorTargetService monitorTargetService;

    public MonitorTargetController(MonitorTargetService monitorTargetService) {
        this.monitorTargetService = monitorTargetService;
    }

    @GetMapping
    public ApiResponse<List<MonitorTarget>> list() {
        return ApiResponse.success(monitorTargetService.list());
    }

    @PostMapping
    public ApiResponse<MonitorTarget> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(monitorTargetService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<MonitorTarget> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(monitorTargetService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id) {
        return ApiResponse.success(Map.of("deleted", monitorTargetService.delete(id)));
    }

    @PostMapping("/{id}/probe")
    public ApiResponse<Map<String, Object>> probe(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.probe(id));
    }

    @PatchMapping("/{id}/stop")
    public ApiResponse<MonitorTarget> stop(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.stopProbe(id));
    }

    @PostMapping("/{id}/stop")
    public ApiResponse<MonitorTarget> stopPost(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.stopProbe(id));
    }

    @PatchMapping("/{id}/resume")
    public ApiResponse<MonitorTarget> resume(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.resumeProbe(id));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<MonitorTarget> resumePost(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.resumeProbe(id));
    }

    @GetMapping("/{id}/metrics")
    public ApiResponse<List<Map<String, Object>>> metrics(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.metrics(id));
    }

    @GetMapping("/{id}/alerts")
    public ApiResponse<List<Map<String, Object>>> alerts(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.alerts(id));
    }

    @GetMapping("/{id}/analyses")
    public ApiResponse<List<Map<String, Object>>> analyses(@PathVariable Long id) {
        return ApiResponse.success(monitorTargetService.analyses(id));
    }
}
