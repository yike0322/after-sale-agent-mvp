package com.yike.aftersaleagent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SessionService sessionService;

    @Test
    void authenticatedDemoUserCreatesSessionAndListsItsMessages() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/sessions")
                        .header("X-Demo-User-Id", "10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andReturn();

        String sessionId = sessionIdFrom(createResult);
        CurrentDemoUser owner = new CurrentDemoUser(10001L, "Demo User 10001");
        assertDoesNotThrow(() -> {
            sessionService.appendUserMessage(owner, sessionId, "Where is my coupon?");
            sessionService.appendAssistantMessage(
                    owner, sessionId, "The threshold is 100.00.", Intent.COUPON_ANALYSIS);
        });

        mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId)
                        .header("X-Demo-User-Id", "10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].intent").doesNotExist())
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[1].intent").value("COUPON_ANALYSIS"));
    }

    @Test
    void sessionIsNotReadableByAnotherDemoUser() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/sessions")
                        .header("X-Demo-User-Id", "10001"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId = sessionIdFrom(createResult);
        mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId)
                        .header("X-Demo-User-Id", "10002"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void nonexistentSessionIsNotFoundForAuthenticatedDemoUser() throws Exception {
        mockMvc.perform(get(
                        "/api/sessions/{sessionId}/messages",
                        "00000000-0000-0000-0000-000000000000")
                        .header("X-Demo-User-Id", "10001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    private String sessionIdFrom(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("sessionId").asText();
    }
}
