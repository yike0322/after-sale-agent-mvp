package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.agent.IntentRouter;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.chat.api.ChatRequest;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.coupon.CouponAnalysisWorkflow;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.knowledge.KnowledgeAnswerService;
import com.yike.aftersaleagent.ticket.RefundSubmission;
import com.yike.aftersaleagent.ticket.RefundWorkflow;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
class DefaultChatOrchestrator implements ChatOrchestrator {
    private static final String RECOGNIZING_STATUS = "正在识别您的问题…";
    private static final String RETRIEVING_STATUS = "正在检索售后规则…";
    private static final String UNSUPPORTED_REPLY =
            "当前演示仅支持售后规则、优惠券问题和退款资格判断。";

    private final SessionService sessionService;
    private final IntentRouter intentRouter;
    private final KnowledgeAnswerService knowledgeAnswerService;
    private final CouponAnalysisWorkflow couponAnalysisWorkflow;
    private final RefundWorkflow refundWorkflow;

    DefaultChatOrchestrator(
            SessionService sessionService,
            IntentRouter intentRouter,
            KnowledgeAnswerService knowledgeAnswerService,
            CouponAnalysisWorkflow couponAnalysisWorkflow,
            RefundWorkflow refundWorkflow) {
        this.sessionService = sessionService;
        this.intentRouter = intentRouter;
        this.knowledgeAnswerService = knowledgeAnswerService;
        this.couponAnalysisWorkflow = couponAnalysisWorkflow;
        this.refundWorkflow = refundWorkflow;
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
            if (intent == Intent.FAQ_QUERY) {
                publisher.status(RETRIEVING_STATUS);
                publisher.message(knowledgeAnswerService.answer(context));
            } else if (intent == Intent.COUPON_ANALYSIS) {
                publisher.message(couponAnalysisWorkflow.execute(context, publisher::status));
            } else if (intent == Intent.REFUND_ELIGIBILITY) {
                RefundSubmission submission = refundWorkflow.submit(context, publisher::status);
                publisher.ticket(submission.ticketId(), submission.currentStatus().name());
                submission.completion().whenComplete((outcome, failure) -> {
                    if (failure == null) {
                        publisher.message(outcome);
                    } else {
                        publisher.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
                    }
                    publisher.done();
                });
                return emitter;
            } else {
                publisher.message(outcome(intent));
            }
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
