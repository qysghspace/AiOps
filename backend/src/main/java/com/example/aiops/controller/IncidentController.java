package com.example.aiops.controller;

import com.example.aiops.common.ApiResponse;
import com.example.aiops.common.AuthContext;
import com.example.aiops.entity.AlertFeedbackSubmitRequest;
import com.example.aiops.entity.IncidentAnalyzeRequest;
import com.example.aiops.entity.IncidentCreateRequest;
import com.example.aiops.entity.IncidentStatusUpdateRequest;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.service.AiFacadeService;
import com.example.aiops.service.AlertFeedbackService;
import com.example.aiops.service.IncidentQueryService;
import com.example.aiops.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final AiFacadeService aiFacadeService;
    private final IncidentService incidentService;
    private final IncidentQueryService incidentQueryService;
    private final AlertFeedbackService alertFeedbackService;

    public IncidentController(AiFacadeService aiFacadeService,
                              IncidentService incidentService,
                              IncidentQueryService incidentQueryService,
                              AlertFeedbackService alertFeedbackService) {
        this.aiFacadeService = aiFacadeService;
        this.incidentService = incidentService;
        this.incidentQueryService = incidentQueryService;
        this.alertFeedbackService = alertFeedbackService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody IncidentCreateRequest request) {
        return ApiResponse.success(incidentService.create(request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody IncidentStatusUpdateRequest request) {
        return ApiResponse.success(incidentService.updateStatus(id, request));
    }

    @GetMapping
    public ApiResponse<List<OpsIncident>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(incidentService.listByStatus(status));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(incidentQueryService.detail(id));
    }

    @GetMapping("/analysis-history")
    public ApiResponse<List<?>> analysisHistory() {
        return ApiResponse.success(incidentQueryService.analysisHistory());
    }

    @GetMapping("/dashboard-stats")
    public ApiResponse<Map<String, Object>> dashboardStats() {
        return ApiResponse.success(incidentQueryService.dashboardStats());
    }

    @PostMapping("/analyze")
    public ApiResponse<Map<String, Object>> analyze(@Valid @RequestBody IncidentAnalyzeRequest request) {
        return ApiResponse.success(aiFacadeService.analyzeIncident(request));
    }

    @PostMapping(value = "/analyze-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@Valid @RequestBody IncidentAnalyzeRequest request) {
        return aiFacadeService.analyzeIncidentStream(request);
    }

    @GetMapping("/feedback/reasons")
    public ApiResponse<List<String>> feedbackReasons() {
        return ApiResponse.success(alertFeedbackService.reasonDict());
    }

    @PostMapping("/feedback")
    public ApiResponse<Map<String, Object>> submitFeedback(@Valid @RequestBody AlertFeedbackSubmitRequest request) {
        return ApiResponse.success(alertFeedbackService.submit(request, AuthContext
                .getUsername()));
    }

    @GetMapping("/{alertId}/feedbacks")
    public ApiResponse<List<Map<String, Object>>> listFeedbacks(@PathVariable Long alertId) {
        return ApiResponse.success(alertFeedbackService.listByAlertId(alertId));
    }

    @GetMapping("/{alertId}/similar-causes")
    public ApiResponse<List<Map<String, Object>>> similarCauses(@PathVariable Long alertId) {
        return ApiResponse.success(alertFeedbackService.similarCauses(alertId));
    }
}
