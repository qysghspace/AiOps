package com.example.aiops.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiCapabilities {

    @Value("${ai.provider.name:ollama}")
    private String providerName;

    @Value("${ai.provider.model:${ai.ollama.model:deepseek-r1:7b}}")
    private String model;

    public String providerName() {
        return providerName;
    }

    public String model() {
        return model;
    }
}
