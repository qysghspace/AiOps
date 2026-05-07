package com.example.aiops.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MobileIncidentBrief {
    private Long id;
    private String incidentNo;
    private String summary;
    private String status;
    private String assignee;
}
