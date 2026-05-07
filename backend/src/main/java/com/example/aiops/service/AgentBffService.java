package com.example.aiops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Service
public class AgentBffService {

    @Value("${ai.agent.base-url:http://localhost:8000}")
    private String aiAgentBaseUrl;

    private final RestTemplate restTemplate;

    public AgentBffService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Map<String, Object> chat(Map<String, Object> body) {
        try {
            Map<String, Object> result = restTemplate.postForObject(aiAgentBaseUrl + "/v1/chat", body, Map.class);
            if (result == null) {
                return Map.of(
                        "answer", "",
                        "structured", Map.of("understanding", "", "nextAction", ""),
                        "toolHistory", java.util.List.of()
                );
            }
            if (!result.containsKey("structured")) {
                result.put("structured", Map.of("understanding", String.valueOf(result.getOrDefault("answer", "")), "nextAction", "请根据排查步骤执行并回填工单"));
            }
            if (!result.containsKey("toolHistory")) {
                result.put("toolHistory", java.util.List.of());
            }
            return result;
        } catch (ResourceAccessException e) {
            return Map.of(
                    "answer", "",
                    "structured", Map.of("understanding", "AI Agent 调用超时或不可达", "nextAction", "请检查 ai-agent 服务端口与运行状态"),
                    "toolHistory", java.util.List.of(),
                    "error", "AI Agent timeout/unreachable: " + e.getMessage()
            );
        } catch (Exception e) {
            return Map.of(
                    "answer", "",
                    "structured", Map.of("understanding", "AI Agent 调用失败", "nextAction", "请查看后端日志定位异常"),
                    "toolHistory", java.util.List.of(),
                    "error", "AI Agent error: " + e.getMessage()
            );
        }
    }

    public SseEmitter chatStream(Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            try {
                Map<String, Object> result = restTemplate.postForObject(aiAgentBaseUrl + "/v1/chat-stream", body, Map.class);
                String answer = String.valueOf(result.getOrDefault("answer", ""));
                if (!answer.isEmpty()) {
                    for (String chunk : answer.split("(?<=\\G.{80})")) {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    }
                }
                emitter.send(SseEmitter.event().name("meta").data(String.valueOf(result)));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("chat-stream failed: " + e.getMessage()));
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (IOException ignored) {
                    // ignore secondary SSE write errors
                }
                // SSE 场景不要抛给全局异常处理器，避免 text/event-stream 被按 JSON 序列化
                try {
                    emitter.complete();
                } catch (IllegalStateException ignored) {
                    // async context may already be completed/errored by container
                }
            }
        }).start();
        return emitter;
    }

    public Map<String, Object> imageAnalyze(String sessionId, MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("sessionId", sessionId);
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            form.add("file", resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.exchange(aiAgentBaseUrl + "/v1/image/analyze", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getMemory(String sessionId) {
        return restTemplate.getForObject(aiAgentBaseUrl + "/v1/sessions/" + sessionId + "/memory", Map.class);
    }

    public Map<String, Object> clearMemory(String sessionId) {
        restTemplate.delete(aiAgentBaseUrl + "/v1/sessions/" + sessionId + "/memory");
        return Map.of("sessionId", sessionId, "cleared", true);
    }

    public Map<String, Object> rollback(String sessionId, Map<String, Object> body) {
        return restTemplate.postForObject(aiAgentBaseUrl + "/v1/sessions/" + sessionId + "/rollback", body, Map.class);
    }
}
