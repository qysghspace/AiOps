package com.example.aiops.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IncidentStatusMachine {

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "OPEN", List.of("IN_PROGRESS"),
            "IN_PROGRESS", List.of("RESOLVED"),
            "RESOLVED", List.of("IN_PROGRESS", "CLOSED"),
            "CLOSED", List.of(),
            "NEW", List.of("ACK", "IN_PROGRESS"),
            "ACK", List.of("IN_PROGRESS"),
            "REVIEWED", List.of()
    );

    public void validate(String fromStatus, String toStatus) {
        List<String> allowed = TRANSITIONS.getOrDefault(fromStatus, List.of());
        if (!allowed.contains(toStatus)) {
            throw new IllegalArgumentException("invalid status transition: " + fromStatus + " -> " + toStatus);
        }
    }
}
