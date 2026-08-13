package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.agent.IntentRouter;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.chat.api.ChatRequest;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
class DefaultChatOrchestrator implements ChatOrchestrator {
    private static final String RECOGNIZING_STATUS = "正在识别您的问题…";
    private static final String UNSUPPORTED_REPLY =
            "当前演示仅支持售后规则、优惠券问题和退款资格判断。";

    private final SessionService sessionService;
    private final IntentRouter intentRouter;

    DefaultChatOrchestrator(SessionService sessionService, IntentRouter intentRouter) {
        this.sessionService = sessionService;
        this.intentRouter = intentRouter;
    }

    @Override
    public SseEmitter stream(ChatRequest request, CurrentDemoUser user) {
        SseEmitter emitter = new SseEmitter();
        SseEventPublisher publisher = new SseEmitterEventPublisher(emitter);
        try {
            AgentExecutionContext context = new AgentExecutionContext(
                    MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY),
                    user.id(),
                    request.sessionId(),
                    request.message());
            sessionService.appendUserMessage(user, request.sessionId(), request.message());
            publisher.status(RECOGNIZING_STATUS);
            Intent intent = intentRouter.route(context);
            publisher.message(outcome(intent));
            publisher.done();
        } catch (BusinessException exception) {
            ErrorCode errorCode = exception.getErrorCode();
            publisher.error(errorCode.getCode(), errorCode.getMessage());
            publisher.done();
        } catch (Exception exception) {
            publisher.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
            publisher.done();
        }
        return emitter;
    }

    private ChatOutcome outcome(Intent intent) {
        String reply = intent == Intent.UNSUPPORTED
                ? UNSUPPORTED_REPLY
                : "已识别为 " + intent.name() + "，后续处理将在下一阶段接入。";
        return new ChatOutcome(reply, null, "ROUTED", List.of());
    }
}
