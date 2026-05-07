package com.example.aiops.service;

import com.example.aiops.entity.HelpAskRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaHelpService {

    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.ollama.model:deepseek-r1:7b}")
    private String ollamaModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> ask(HelpAskRequest request) {
        String url = ollamaBaseUrl + "/api/generate";

        Map<String, Object> body = buildAskBody(request, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> resp = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> result = new HashMap<>();
        result.put("model", ollamaModel);
        result.put("answer", resp == null ? "" : String.valueOf(resp.getOrDefault("response", "")));
        return result;
    }

    public SseEmitter askStream(HelpAskRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);
        new Thread(() -> {
            try {
                Map<String, Object> nonStream = ask(request);
                String full = String.valueOf(nonStream.getOrDefault("answer", ""));
                int chunkSize = 32;
                for (int i = 0; i < full.length(); i += chunkSize) {
                    int end = Math.min(full.length(), i + chunkSize);
                    String chunk = full.substring(i, end);
                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    Thread.sleep(40);
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    private Map<String, Object> buildAskBody(HelpAskRequest request, boolean stream) {
        String systemPrompt = "你是AIOps项目的新手指导助手。严格使用中文并使用简洁步骤型输出。"
                + "请按固定结构回答：\n"
                + "问题判断：一句话\n"
                + "可能原因：最多3条\n"
                + "排查步骤：按1/2/3列出\n"
                + "建议操作：最多3条\n"
                + "如果问题涉及数据输入，明确指出数据来源（监控/日志/APM/人工输入）。\n"
                + "避免空泛描述，不要输出代码块。";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("用户问题：").append(request.getQuestion());
        if (request.getContext() != null && !request.getContext().isBlank()) {
            userPrompt.append("\n上下文：").append(request.getContext());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", ollamaModel);
        body.put("prompt", systemPrompt + "\n\n" + userPrompt);
        body.put("stream", stream);
        return body;
    }
}
