package com.example.aiops.service;

import java.util.function.Consumer;

public interface AiProviderService {

    String chat(String systemPrompt, String userPrompt);

    default String streamChat(String systemPrompt, String userPrompt, Consumer<String> chunkConsumer) {
        String answer = chat(systemPrompt, userPrompt);
        if (answer != null && !answer.isEmpty()) {
            chunkConsumer.accept(answer);
        }
        return answer;
    }
}
