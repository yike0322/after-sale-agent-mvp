package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.ai.MockAiGateway;
import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.coupon.CouponAnalysisWorkflow;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.knowledge.KnowledgeRetriever;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
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

    @MockitoBean
    KnowledgeRetriever knowledgeRetriever;

    @MockitoBean
    CouponAnalysisWorkflow couponAnalysisWorkflow;

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
        given(knowledgeRetriever.retrieve(eq(Intent.FAQ_QUERY), eq("NORMAL"), any(), eq(3)))
                .willReturn(List.of(new SourceCitation(
                        "售后服务规则",
                        "knowledge/after-sale-rule.md",
                        "普通商品签收后 7 天内，未使用且无损坏时可申请退货。")));

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
                .contains(
                        "event:status",
                        "正在检索售后规则…",
                        "knowledge/after-sale-rule.md",
                        "event:done")
                .containsSubsequence("正在识别您的问题…", "正在检索售后规则…", "event:message", "event:done")
                .endsWith("event:done\ndata:[DONE]\n\n");
        assertThat(aiGateways).singleElement().isInstanceOf(MockAiGateway.class);
    }

    @Test
    void couponAnalysisStreamsActualWorkflowStatusesThenTheDeterministicOutcome() throws Exception {
        willAnswer(invocation -> {
            Consumer<String> onStepStarted = invocation.getArgument(1);
            onStepStarted.accept("正在查询订单…");
            onStepStarted.accept("正在查询优惠券…");
            onStepStarted.accept("正在校验优惠券规则…");
            return new com.yike.aftersaleagent.chat.api.ChatOutcome(
                    "系统规则结论：订单金额 80.00 元，未达到优惠券使用门槛 100.00 元。",
                    null,
                    "ORDER_AMOUNT_BELOW_THRESHOLD",
                    List.of());
        }).given(couponAnalysisWorkflow).execute(any(AgentExecutionContext.class), any());
        String sessionId = sessionService.createSession(USER);

        MvcResult started = mockMvc.perform(post("/api/chat/stream")
                        .header("X-Demo-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"sessionId\":\"" + sessionId
                                + "\",\"message\":\"订单 O1001 使用优惠券 C1001\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String stream = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(stream)
                .contains("\"ticketId\":null", "\"taskStatus\":\"ORDER_AMOUNT_BELOW_THRESHOLD\"", "\"citations\":[]")
                .doesNotContain("event:ticket")
                .containsSubsequence(
                        "正在识别您的问题…",
                        "正在查询订单…",
                        "正在查询优惠券…",
                        "正在校验优惠券规则…",
                        "event:message",
                        "ORDER_AMOUNT_BELOW_THRESHOLD",
                        "event:done")
                .endsWith("event:done\ndata:[DONE]\n\n");
    }

    @Test
    void couponReferenceFailureEmitsOnlyTheExistingErrorAndTerminalDone() throws Exception {
        given(couponAnalysisWorkflow.execute(any(AgentExecutionContext.class), any()))
                .willThrow(new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED));
        String sessionId = sessionService.createSession(USER);

        MvcResult started = mockMvc.perform(post("/api/chat/stream")
                        .header("X-Demo-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"sessionId\":\"" + sessionId
                                + "\",\"message\":\"我要使用优惠券，但没有订单号\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String stream = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(stream)
                .contains("正在识别您的问题…", "event:error", "BUSINESS_REFERENCE_REQUIRED", "event:done")
                .doesNotContain("正在查询订单…", "正在查询优惠券…", "正在校验优惠券规则…", "event:message")
                .containsSubsequence("正在识别您的问题…", "event:error", "event:done")
                .endsWith("event:done\ndata:[DONE]\n\n");
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
