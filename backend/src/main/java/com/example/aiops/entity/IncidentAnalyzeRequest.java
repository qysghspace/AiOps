package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IncidentAnalyzeRequest {

    @NotBlank(message = "incidentId is required")
    private String incidentId;

    @NotBlank(message = "summary is required")
    private String summary;
}
