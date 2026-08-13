package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.order.OrderQuery;
import com.yike.aftersaleagent.order.OrderQueryTool;
import com.yike.aftersaleagent.order.OrderSummary;
import com.yike.aftersaleagent.ticket.domain.AgentStepStatus;
import com.yike.aftersaleagent.ticket.domain.RefundDecision;
import com.yike.aftersaleagent.ticket.domain.RefundWorkflowStep;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service("refundTaskRunner")
public class RefundTaskExecutor {
    private static final String QUERY_ORDER_STATUS = "正在查询订单…";
    private static final String RETRIEVE_RULE_STATUS = "正在检索售后规则…";
    private static final String EVALUATE_STATUS = "正在判定退款资格…";
    private static final String UPDATE_TICKET_STATUS = "正在更新人工审核工单…";
    private static final String GENERATE_SUMMARY_STATUS = "正在生成处理摘要…";

    private final ToolRegistry toolRegistry;
    private final OrderQueryTool orderQueryTool;
    private final AfterSaleRuleQueryTool afterSaleRuleQueryTool;
    private final RefundRuleService refundRuleService;
    private final AgentStepLogService agentStepLogService;
    private final TicketTaskStateService ticketTaskStateService;
    private final TicketAuditLinkService ticketAuditLinkService;

    public RefundTaskExecutor(
            ToolRegistry toolRegistry,
            OrderQueryTool orderQueryTool,
            AfterSaleRuleQueryTool afterSaleRuleQueryTool,
            RefundRuleService refundRuleService,
            AgentStepLogService agentStepLogService,
            TicketTaskStateService ticketTaskStateService,
            TicketAuditLinkService ticketAuditLinkService) {
        this.toolRegistry = toolRegistry;
        this.orderQueryTool = orderQueryTool;
        this.afterSaleRuleQueryTool = afterSaleRuleQueryTool;
        this.refundRuleService = refundRuleService;
        this.agentStepLogService = agentStepLogService;
        this.ticketTaskStateService = ticketTaskStateService;
        this.ticketAuditLinkService = ticketAuditLinkService;
    }

    @Async("refundTaskExecutor")
    public CompletableFuture<ChatOutcome> execute(
            AgentExecutionContext context,
            long taskId,
            long ticketId,
            String orderNo,
            Consumer<String> statusSink) {
        try {
            ticketTaskStateService.transition(context.userId(), ticketId, taskId,
                    TicketTaskStatus.PENDING, TicketTaskStatus.RUNNING, 0, "处理中");
            statusSink.accept(QUERY_ORDER_STATUS);
            OrderSummary order = queryOrder(context, ticketId, orderNo);
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.QUERY_ORDER,
                    AgentStepStatus.SUCCESS, AgentStepLogPayload.queryOrderSucceeded());

            statusSink.accept(RETRIEVE_RULE_STATUS);
            List<SourceCitation> evidence = queryRules(context, ticketId, order.productType());
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.RETRIEVE_AFTER_SALE_RULE,
                    AgentStepStatus.SUCCESS, AgentStepLogPayload.ruleQuerySucceeded(evidence.size()));

            statusSink.accept(EVALUATE_STATUS);
            RefundDecision decision = refundRuleService.evaluate(order, evidence);
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.EVALUATE_REFUND_ELIGIBILITY,
                    AgentStepStatus.SUCCESS, AgentStepLogPayload.decision(decision));

            statusSink.accept(UPDATE_TICKET_STATUS);
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.CREATE_OR_UPDATE_HUMAN_REVIEW_TICKET,
                    AgentStepStatus.SUCCESS, AgentStepLogPayload.ticketUpdated(decision.ticketStatus()));

            statusSink.accept(GENERATE_SUMMARY_STATUS);
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.GENERATE_USER_SUMMARY,
                    AgentStepStatus.SUCCESS, AgentStepLogPayload.summaryGenerated(true));
            ticketTaskStateService.transition(context.userId(), ticketId, taskId,
                    TicketTaskStatus.RUNNING, decision.ticketStatus(), 5, decision.reasonText());
            return CompletableFuture.completedFuture(new ChatOutcome(
                    decision.reasonText(), ticketId, decision.ticketStatus().name(), evidence));
        } catch (BusinessException exception) {
            return CompletableFuture.completedFuture(failSafely(context, ticketId, taskId, exception));
        } catch (RuntimeException exception) {
            return CompletableFuture.completedFuture(failSafely(context, ticketId, taskId,
                    new BusinessException(ErrorCode.INTERNAL_ERROR)));
        }
    }

    private OrderSummary queryOrder(AgentExecutionContext context, long ticketId, String orderNo) {
        try {
            return toolRegistry.execute(Intent.REFUND_ELIGIBILITY, orderQueryTool, context, new OrderQuery(orderNo));
        } catch (BusinessException exception) {
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.QUERY_ORDER,
                    AgentStepStatus.FAILED, AgentStepLogPayload.queryOrderFailed());
            throw exception;
        } finally {
            ticketAuditLinkService.linkRefundAuditAttempts(context.userId(), ticketId, context.requestId());
        }
    }

    private List<SourceCitation> queryRules(AgentExecutionContext context, long ticketId, String productType) {
        try {
            return toolRegistry.execute(Intent.REFUND_ELIGIBILITY, afterSaleRuleQueryTool, context,
                    new AfterSaleRuleQuery(productType));
        } catch (BusinessException exception) {
            agentStepLogService.appendCompleted(ticketId, RefundWorkflowStep.RETRIEVE_AFTER_SALE_RULE,
                    AgentStepStatus.FAILED, AgentStepLogPayload.ruleQueryFailed());
            throw exception;
        } finally {
            ticketAuditLinkService.linkRefundAuditAttempts(context.userId(), ticketId, context.requestId());
        }
    }

    private ChatOutcome failSafely(AgentExecutionContext context, long ticketId, long taskId, BusinessException failure) {
        try {
            ticketTaskStateService.transition(context.userId(), ticketId, taskId,
                    TicketTaskStatus.RUNNING, TicketTaskStatus.WAIT_HUMAN, 1,
                    "处理异常，已转人工审核");
            return new ChatOutcome("系统暂时无法完成自动核验，已转入人工审核。", ticketId,
                    TicketTaskStatus.WAIT_HUMAN.name(), List.of());
        } catch (RuntimeException transitionFailure) {
            return new ChatOutcome("系统暂时无法完成处理，请稍后重试。", ticketId,
                    TicketTaskStatus.FAILED.name(), List.of());
        }
    }
}
