package com.example.aiops.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aiops.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isWhitelisted(path)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        String token = extractBearerToken(authHeader);
        String username = authService.verifyAndGetUsername(token);
        if (username == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> body = new HashMap<>();
            body.put("code", "401");
            body.put("message", "unauthorized");
            body.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return false;
        }

        AuthContext.setToken(token);
        AuthContext.setUsername(username);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isWhitelisted(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/mobile/auth/wx-login")
                || path.startsWith("/actuator/health")
                || path.startsWith("/error");
    }
}
