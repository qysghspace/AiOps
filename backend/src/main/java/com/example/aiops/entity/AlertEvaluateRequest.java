package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertEvaluateRequest {

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotNull(message = "errorRate is required")
    private Double errorRate;

    @NotNull(message = "latencyMs is required")
    private Long latencyMs;

    @NotBlank(message = "summary is required")
    private String summary;
}
