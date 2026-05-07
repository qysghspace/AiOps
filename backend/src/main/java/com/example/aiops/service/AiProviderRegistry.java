package com.example.aiops.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiProviderRegistry {

    private final AiProviderService aiProviderService;

    public AiProviderRegistry(@Qualifier("ollamaDeepSeekR1AiProviderService") AiProviderService aiProviderService) {
        this.aiProviderService = aiProviderService;
    }

    public AiProviderService current() {
        return aiProviderService;
    }
}
