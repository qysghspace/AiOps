package com.example.aiops.controller;

import com.example.aiops.common.ApiResponse;
import com.example.aiops.service.AgentBffService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentBffService agentBffService;

    public AgentController(AgentBffService agentBffService) {
        this.agentBffService = agentBffService;
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(agentBffService.chat(body));
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        return agentBffService.chatStream(body);
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> image(@RequestParam("sessionId") String sessionId,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(agentBffService.imageAnalyze(sessionId, file));
    }

    @GetMapping("/sessions/{sessionId}/memory")
    public ApiResponse<Map<String, Object>> getMemory(@PathVariable String sessionId) {
        return ApiResponse.success(agentBffService.getMemory(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}/memory")
    public ApiResponse<Map<String, Object>> clearMemory(@PathVariable String sessionId) {
        return ApiResponse.success(agentBffService.clearMemory(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/rollback")
    public ApiResponse<Map<String, Object>> rollback(@PathVariable String sessionId,
                                                     @RequestBody Map<String, Object> body) {
        return ApiResponse.success(agentBffService.rollback(sessionId, body));
    }
}
