package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.HelpAskRequest;
import com.example.aiops.entity.IncidentAnalyzeRequest;
import com.example.aiops.entity.OpsAiAnalysis;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.mapper.OpsAiAnalysisMapper;
import com.example.aiops.mapper.OpsIncidentMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiFacadeService {

    private final AiProviderRegistry aiProviderRegistry;
    private final AiCapabilities aiCapabilities;
    private final AiPromptService aiPromptService;
    private final OpsIncidentMapper opsIncidentMapper;
    private final OpsAiAnalysisMapper opsAiAnalysisMapper;

    public AiFacadeService(AiProviderRegistry aiProviderRegistry,
                           AiCapabilities aiCapabilities,
                           AiPromptService aiPromptService,
                           OpsIncidentMapper opsIncidentMapper,
                           OpsAiAnalysisMapper opsAiAnalysisMapper) {
        this.aiProviderRegistry = aiProviderRegistry;
        this.aiCapabilities = aiCapabilities;
        this.aiPromptService = aiPromptService;
        this.opsIncidentMapper = opsIncidentMapper;
        this.opsAiAnalysisMapper = opsAiAnalysisMapper;
    }

    public Map<String, Object> askHelp(HelpAskRequest request) {
        String answer = aiProviderRegistry.current().chat(aiPromptService.buildHelpSystemPrompt(), aiPromptService.buildHelpUserPrompt(request));
        Map<String, Object> result = new HashMap<>();
        result.put("model", aiCapabilities.model());
        result.put("answer", answer);
        result.put("provider", aiCapabilities.providerName());
        result.put("feature", "help");
        return result;
    }

    public Map<String, Object> analyzeIncident(IncidentAnalyzeRequest request) {
        String answer = aiProviderRegistry.current().chat(aiPromptService.buildIncidentSystemPrompt(), aiPromptService.buildIncidentUserPrompt(request));
        Long incidentId = resolveIncidentId(request.getIncidentId());
        persistAnalysis(incidentId, answer);
        Map<String, Object> result = new HashMap<>();
        result.put("model", aiCapabilities.model());
        result.put("answer", answer);
        result.put("stored", true);
        result.put("storedIncidentId", incidentId);
        result.put("provider", aiCapabilities.providerName());
        result.put("feature", "incident-analyze");
        return result;
    }

    public SseEmitter askHelpStream(HelpAskRequest request) {
        return streamAnswer("help", null, aiPromptService.buildHelpSystemPrompt(), aiPromptService.buildHelpUserPrompt(request));
    }

    public SseEmitter analyzeIncidentStream(IncidentAnalyzeRequest request) {
        Long incidentId = resolveIncidentId(request.getIncidentId());
        return streamAnswer("incident-analyze", incidentId, aiPromptService.buildIncidentSystemPrompt(), aiPromptService.buildIncidentUserPrompt(request));
    }

    private SseEmitter streamAnswer(String feature, Long incidentId, String systemPrompt, String userPrompt) {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            StringBuilder answer = new StringBuilder();
            try {
                sendEvent(emitter, "meta", Map.of(
                        "provider", aiCapabilities.providerName(),
                        "model", aiCapabilities.model(),
                        "feature", feature
                ));
                String fullAnswer = aiProviderRegistry.current().streamChat(systemPrompt, userPrompt, chunk -> {
                    answer.append(chunk);
                    sendEvent(emitter, "chunk", chunk);
                });
                String finalAnswer = answer.isEmpty() ? fullAnswer : answer.toString();
                if (incidentId != null) {
                    persistAnalysis(incidentId, finalAnswer);
                }
                sendEvent(emitter, "done", Map.of(
                        "stored", incidentId != null,
                        "storedIncidentId", incidentId == null ? "" : incidentId
                ));
                emitter.complete();
            } catch (Exception ex) {
                sendEvent(emitter, "error", ex.getMessage() == null ? "AI stream failed" : ex.getMessage());
                emitter.completeWithError(ex);
            }
        }, "ai-sse-" + feature).start();
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to send sse event", ex);
        }
    }

    private Long resolveIncidentId(String incidentNoOrId) {
        try {
            return Long.parseLong(incidentNoOrId);
        } catch (NumberFormatException ex) {
            OpsIncident incident = opsIncidentMapper.selectOne(new LambdaQueryWrapper<OpsIncident>()
                    .eq(OpsIncident::getIncidentNo, incidentNoOrId)
                    .last("limit 1"));
            if (incident == null) {
                throw new IllegalArgumentException("incident not found by id or incidentNo: " + incidentNoOrId);
            }
            return incident.getId();
        }
    }

    private void persistAnalysis(Long incidentId, String answer) {
        OpsAiAnalysis row = new OpsAiAnalysis();
        row.setIncidentId(incidentId);
        row.setConfidence(new BigDecimal("80"));
        row.setRootCause(answer);
        row.setSuggestion(answer);
        opsAiAnalysisMapper.insert(row);
    }
}
