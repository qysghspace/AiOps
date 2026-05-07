package com.example.aiops.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertRuleUpsertRequest {

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotNull(message = "errorRateThreshold is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "errorRateThreshold must be > 0")
    private Double errorRateThreshold;

    @NotNull(message = "latencyThresholdMs is required")
    @Min(value = 1, message = "latencyThresholdMs must be > 0")
    private Long latencyThresholdMs;

    @NotNull(message = "dedupWindowSec is required")
    @Min(value = 1, message = "dedupWindowSec must be > 0")
    private Long dedupWindowSec;

    @NotNull(message = "suppressWindowSec is required")
    @Min(value = 1, message = "suppressWindowSec must be > 0")
    private Long suppressWindowSec;

    @NotBlank(message = "enabled is required")
    private String enabled;
}
