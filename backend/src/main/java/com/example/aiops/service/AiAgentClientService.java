package com.example.aiops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aiops.entity.IncidentAnalyzeRequest;
import com.example.aiops.entity.OpsAiAnalysis;
import com.example.aiops.entity.OpsIncident;
import com.example.aiops.mapper.OpsAiAnalysisMapper;
import com.example.aiops.mapper.OpsIncidentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAgentClientService {

    @Value("${ai.agent.base-url:http://localhost:8000}")
    private String aiAgentBaseUrl;

    private final OpsIncidentMapper opsIncidentMapper;
    private final OpsAiAnalysisMapper opsAiAnalysisMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiAgentClientService(OpsIncidentMapper opsIncidentMapper, OpsAiAnalysisMapper opsAiAnalysisMapper) {
        this.opsIncidentMapper = opsIncidentMapper;
        this.opsAiAnalysisMapper = opsAiAnalysisMapper;
    }

    public Map<String, Object> analyzeIncident(IncidentAnalyzeRequest request) {
        String url = aiAgentBaseUrl + "/agent/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("incidentId", request.getIncidentId());
        body.put("summary", request.getSummary());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> aiResult = restTemplate.postForObject(url, entity, Map.class);

        Long incidentId = resolveIncidentId(request.getIncidentId());
        persistAnalysis(incidentId, aiResult);
        aiResult.put("stored", true);
        aiResult.put("storedIncidentId", incidentId);
        return aiResult;
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

    private void persistAnalysis(Long incidentId, Map<String, Object> aiResult) {
        OpsAiAnalysis row = new OpsAiAnalysis();
        row.setIncidentId(incidentId);

        Object confidence = aiResult.get("confidence");
        if (confidence != null) {
            row.setConfidence(new BigDecimal(String.valueOf(confidence)));
        }

        Object candidates = aiResult.get("rootCauseCandidates");
        Object actions = aiResult.get("actions");
        row.setRootCause(toFlatString(candidates));
        row.setSuggestion(toFlatString(actions));
        opsAiAnalysisMapper.insert(row);
    }

    private String toFlatString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.toString();
        }
        return String.valueOf(value);
    }
}
