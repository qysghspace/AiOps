package com.example.aiops.controller;

import com.example.aiops.common.ApiResponse;
import com.example.aiops.common.AuthContext;
import com.example.aiops.entity.ChangePasswordRequest;
import com.example.aiops.entity.LoginRequest;
import com.example.aiops.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.login(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(AuthContext.getToken());
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Map<String, Object> result = new HashMap<>();
        result.put("username", AuthContext.getUsername());
        return ApiResponse.success(result);
    }

    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.success(authService.changePassword(AuthContext.getUsername(), request));
    }

    @GetMapping("/login-logs")
    public ApiResponse<List<?>> loginLogs() {
        return ApiResponse.success((List<?>) authService.recentLoginLogs(AuthContext.getUsername()));
    }
}
