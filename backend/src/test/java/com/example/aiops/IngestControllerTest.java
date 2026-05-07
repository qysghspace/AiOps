package com.example.aiops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIngestLogSuccessfully() throws Exception {
        String body = """
                {
                  \"serviceName\": \"order-service\",
                  \"environment\": \"prod\",
                  \"level\": \"ERROR\",
                  \"message\": \"db timeout\",
                  \"timestamp\": 1713860000000,
                  \"traceId\": \"trace-001\"
                }
                """;

        mockMvc.perform(post("/api/logs/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accepted").value(true));
    }
}
