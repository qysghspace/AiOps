package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HelpAskRequest {

    @NotBlank(message = "question is required")
    private String question;

    private String context;
}
