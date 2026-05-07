package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogIngestRequest {

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotBlank(message = "environment is required")
    private String environment;

    @NotBlank(message = "level is required")
    private String level;

    @NotBlank(message = "message is required")
    private String message;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    private String traceId;
}
