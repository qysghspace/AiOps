package com.example.aiops.service;

import com.example.aiops.entity.LogIngestRequest;
import com.example.aiops.entity.OpsLogEvent;
import com.example.aiops.mapper.OpsLogEventMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class IngestService {

    private final OpsLogEventMapper opsLogEventMapper;

    public IngestService(OpsLogEventMapper opsLogEventMapper) {
        this.opsLogEventMapper = opsLogEventMapper;
    }

    public Map<String, Object> ingestLog(LogIngestRequest request) {
        OpsLogEvent event = new OpsLogEvent();
        event.setServiceName(request.getServiceName());
        event.setEnvironment(request.getEnvironment());
        event.setLevel(request.getLevel());
        event.setMessage(request.getMessage());
        event.setTraceId(request.getTraceId());
        event.setEventTime(request.getTimestamp());
        opsLogEventMapper.insert(event);

        Map<String, Object> result = new HashMap<>();
        result.put("accepted", true);
        result.put("id", event.getId());
        result.put("serviceName", request.getServiceName());
        result.put("traceId", request.getTraceId());
        result.put("receivedAt", System.currentTimeMillis());
        return result;
    }
}
