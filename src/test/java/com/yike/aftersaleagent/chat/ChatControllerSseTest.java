package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.ai.MockAiGateway;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerSseTest {
    private static final CurrentDemoUser USER = new CurrentDemoUser(10001L, "Demo User 10001");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SessionService sessionService;

    @Autowired
    List<AiGateway> aiGateways;

    @Test
    void streamsNamedStatusMessageAndTerminalDoneEventsForAnOwnedSession() throws Exception {
        String sessionId = sessionService.createSession(USER);

        MvcResult started = mockMvc.perform(post("/api/chat/stream")
                        .header("X-Demo-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"sessionId\":\"" + sessionId + "\",\"message\":\"你好\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String stream = completed.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(stream)
                .contains(
                        "event:status",
                        "正在识别您的问题…",
                        "event:message",
                        "当前演示仅支持售后规则、优惠券问题和退款资格判断。",
                        "event:done")
                .containsSubsequence("event:status", "event:message", "event:done")
                .endsWith("event:done\ndata:[DONE]\n\n");
        assertThat(sessionService.listMessages(USER, sessionId))
                .extracting(message -> message.getContent())
                .isEqualTo(List.of("你好"));
    }

    @Test
    void supportedIntentAlsoTerminatesNormallyWithTheSingleMockGateway() throws Exception {
        String sessionId = sessionService.createSession(USER);

        MvcResult started = mockMvc.perform(post("/api/chat/stream")
                        .header("X-Demo-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"sessionId\":\"" + sessionId
                                + "\",\"message\":\"售后规则是什么？\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String stream = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(stream)
                .contains("event:status", "FAQ_QUERY", "event:done")
                .containsSubsequence("event:status", "event:message", "event:done")
                .endsWith("event:done\ndata:[DONE]\n\n");
        assertThat(aiGateways).singleElement().isInstanceOf(MockAiGateway.class);
    }

    @Test
    void foreignSessionEmitsProjectErrorCodeThenTerminates() throws Exception {
        String foreignSessionId = sessionService.createSession(
                new CurrentDemoUser(10002L, "Demo User 10002"));

        MvcResult started = mockMvc.perform(post("/api/chat/stream")
                        .header("X-Demo-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"sessionId\":\"" + foreignSessionId
                                + "\",\"message\":\"售后规则是什么？\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String stream = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(stream)
                .contains("event:error", "SESSION_NOT_FOUND")
                .doesNotContain("event:status", "event:message")
                .endsWith("event:done\ndata:[DONE]\n\n");
    }

    @Test
    void rejectsBlankChatRequestsAtTheMvcBoundary() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .header("X-Demo-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\" \",\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
