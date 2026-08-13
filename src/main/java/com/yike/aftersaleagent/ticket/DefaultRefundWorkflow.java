package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.agent.BusinessReferenceExtractor;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

import static com.yike.aftersaleagent.agent.Intent.REFUND_ELIGIBILITY;

@Service
public class DefaultRefundWorkflow implements RefundWorkflow {
    private static final String CREATE_TICKET_STATUS = "正在创建退款审核工单…";

    private final BusinessReferenceExtractor referenceExtractor;
    private final RefundIdempotencyKeyFactory idempotencyKeyFactory;
    private final ToolRegistry toolRegistry;
    private final TicketCreateTool ticketCreateTool;
    private final TicketTaskMapper ticketTaskMapper;
    private final TicketAuditLinkService ticketAuditLinkService;
    private final RefundTaskExecutor refundTaskExecutor;

    public DefaultRefundWorkflow(
            BusinessReferenceExtractor referenceExtractor,
            RefundIdempotencyKeyFactory idempotencyKeyFactory,
            ToolRegistry toolRegistry,
            TicketCreateTool ticketCreateTool,
            TicketTaskMapper ticketTaskMapper,
            TicketAuditLinkService ticketAuditLinkService,
            RefundTaskExecutor refundTaskExecutor) {
        this.referenceExtractor = referenceExtractor;
        this.idempotencyKeyFactory = idempotencyKeyFactory;
        this.toolRegistry = toolRegistry;
        this.ticketCreateTool = ticketCreateTool;
        this.ticketTaskMapper = ticketTaskMapper;
        this.ticketAuditLinkService = ticketAuditLinkService;
        this.refundTaskExecutor = refundTaskExecutor;
    }

    @Override
    public RefundSubmission submit(AgentExecutionContext context, Consumer<String> statusSink) {
        String orderNo = referenceExtractor.requireOrderNo(context.userMessage());
        String key = idempotencyKeyFactory.create(context.userId(), context.sessionId(), orderNo);
        Consumer<String> safeStatusSink = statusSink == null ? ignored -> { } : statusSink;
        safeStatusSink.accept(CREATE_TICKET_STATUS);
        TicketCreateResult created = toolRegistry.execute(REFUND_ELIGIBILITY, ticketCreateTool, context,
                new TicketCreateRequest(orderNo, key));
        ticketAuditLinkService.linkRefundAuditAttempts(context.userId(), created.ticketId(), context.requestId());
        if (!created.newlySubmitted()) {
            return new RefundSubmission(created.ticketId(), false, created.currentStatus(),
                    CompletableFuture.completedFuture(existingTicketOutcome(created)));
        }
        TicketTaskRecord task = ticketTaskMapper.findOwnedByIdempotency(context.userId(), key);
        if (task == null || task.ticketId() != created.ticketId()) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
        return new RefundSubmission(created.ticketId(), true, created.currentStatus(),
                refundTaskExecutor.execute(context, task.taskId(), created.ticketId(), orderNo, safeStatusSink));
    }

    private ChatOutcome existingTicketOutcome(TicketCreateResult created) {
        return new ChatOutcome("该退款审核工单已提交，请通过工单详情查看处理进度。", created.ticketId(),
                created.currentStatus().name(), List.of());
    }
}
