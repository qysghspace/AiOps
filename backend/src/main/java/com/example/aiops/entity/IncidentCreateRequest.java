package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IncidentCreateRequest {

    @NotNull(message = "alertId is required")
    private Long alertId;

    @NotBlank(message = "summary is required")
    private String summary;

    private String assignee;
}
