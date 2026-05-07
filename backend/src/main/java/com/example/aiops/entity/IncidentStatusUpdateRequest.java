package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IncidentStatusUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

    private String assignee;
}
