package com.example.aiops.service;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class OllamaDeepSeekR1AiProviderService implements AiProviderService {

    @Value("${ai.provider.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ai.provider.model:deepseek-r1:7b}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        String url = baseUrl + "/api/chat";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> resp = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        if (resp == null) {
            return "";
        }
        Object message = resp.get("message");
        if (message instanceof Map<?, ?> map) {
            Object content = map.get("content");
            return content == null ? "" : String.valueOf(content);
        }
        return String.valueOf(resp.getOrDefault("response", ""));
    }

    @Override
    public String streamChat(String systemPrompt, String userPrompt, Consumer<String> chunkConsumer) {
        String url = baseUrl + "/api/chat";
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", true);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        StringBuilder answer = new StringBuilder();
        restTemplate.execute(url, org.springframework.http.HttpMethod.POST, request -> {
            request.getHeaders().putAll(headers);
            objectMapper.writeValue(request.getBody(), body);
        }, (ClientHttpResponse response) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode root = objectMapper.readTree(line);
                    JsonNode contentNode = root.path("message").path("content");
                    if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                        String chunk = contentNode.asText();
                        if (!chunk.isEmpty()) {
                            answer.append(chunk);
                            chunkConsumer.accept(chunk);
                        }
                    }
                }
            }
            return null;
        });
        return answer.toString();
    }
}
