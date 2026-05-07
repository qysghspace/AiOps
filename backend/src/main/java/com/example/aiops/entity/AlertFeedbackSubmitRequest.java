package com.example.aiops.entity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AlertFeedbackSubmitRequest {

    @NotNull
    private Long alertId;

    private Long incidentId;

    private Long aiAnalysisId;

    private List<String> selectedReasons;

    private String reasonText;

    private Boolean falsePositive;
}
