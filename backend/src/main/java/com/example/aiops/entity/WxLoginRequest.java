package com.example.aiops.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginRequest {

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "username is required")
    private String username;
}
