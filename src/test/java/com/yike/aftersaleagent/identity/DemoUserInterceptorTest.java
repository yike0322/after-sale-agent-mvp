package com.yike.aftersaleagent.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoUserInterceptorTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void missingDemoUserHeaderIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DEMO_USER_REQUIRED"));
    }

    @Test
    void unknownDemoUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/sessions").header("X-Demo-User-Id", "99999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DEMO_USER_NOT_FOUND"));
    }
}
