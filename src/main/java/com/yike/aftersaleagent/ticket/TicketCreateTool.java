package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.GovernedTool;
import com.yike.aftersaleagent.tool.ToolAuditRequest;
import com.yike.aftersaleagent.tool.ToolAuditService;
import com.yike.aftersaleagent.tool.ToolRisk;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TicketCreateTool implements GovernedTool<TicketCreateRequest, TicketCreateResult> {
    private static final Pattern ORDER_NO = Pattern.compile("O\\d{4,}");
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("refund:v1:[0-9a-f]{64}");
    private static final String PENDING = TicketTaskStatus.PENDING.name();

    private final TicketMapper ticketMapper;
    private final TicketTaskMapper ticketTaskMapper;
    private final ToolAuditService toolAuditService;

    public TicketCreateTool(
            TicketMapper ticketMapper, TicketTaskMapper ticketTaskMapper, ToolAuditService toolAuditService) {
        this.ticketMapper = ticketMapper;
        this.ticketTaskMapper = ticketTaskMapper;
        this.toolAuditService = toolAuditService;
    }

    @Override
    public String name() { return "ticketCreate"; }

    @Override
    public ToolRisk risk() { return ToolRisk.HUMAN_APPROVAL_REQUIRED; }

    @Override
    @Transactional
    public TicketCreateResult execute(AgentExecutionContext context, TicketCreateRequest request) {
        String orderNo = normalizeOrderNo(request);
        String idempotencyKey = validateKey(request);
        TicketTaskRecord existing = ticketTaskMapper.findOwnedByIdempotency(context.userId(), idempotencyKey);
        if (existing != null) {
            TicketCreateResult reused = new TicketCreateResult(
                    existing.ticketId(), false, TicketTaskStatus.valueOf(existing.status()));
            auditSuccess(context, orderNo, reused);
            return reused;
        }
        try {
            CustomerTicket ticket = pendingTicket(context.userId(), orderNo);
            if (ticketMapper.insert(ticket) != 1 || ticket.getId() == null) {
                throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
            }
            TicketTask task = pendingTask(ticket.getId(), idempotencyKey);
            if (ticketTaskMapper.insert(task) != 1) {
                throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
            }
            TicketCreateResult created = new TicketCreateResult(ticket.getId(), true, TicketTaskStatus.PENDING);
            auditSuccess(context, orderNo, created);
            return created;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
    }

    private void auditSuccess(AgentExecutionContext context, String orderNo, TicketCreateResult result) {
        try {
            toolAuditService.recordSuccess(name(), ToolAuditRequest.success(
                    context.requestId(), "orderNo=" + orderNo,
                    "ticketId=" + result.ticketId() + ",created=" + result.newlySubmitted()), 0L);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
    }

    private CustomerTicket pendingTicket(long userId, String orderNo) {
        CustomerTicket ticket = new CustomerTicket();
        ticket.setUserId(userId);
        ticket.setTicketType("REFUND_REVIEW");
        ticket.setPriority("NORMAL");
        ticket.setStatus(PENDING);
        ticket.setTitle("退款资格人工审核");
        ticket.setDescription("orderNo=" + orderNo);
        return ticket;
    }

    private TicketTask pendingTask(long ticketId, String idempotencyKey) {
        TicketTask task = new TicketTask();
        task.setTicketId(ticketId);
        task.setIdempotencyKey(idempotencyKey);
        task.setStatus(PENDING);
        task.setCurrentStep(0);
        task.setTotalSteps(5);
        return task;
    }

    private String normalizeOrderNo(TicketCreateRequest request) {
        String value = request == null || request.normalizedOrderNo() == null ? "" : request.normalizedOrderNo().strip().toUpperCase(Locale.ROOT);
        if (!ORDER_NO.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        }
        return value;
    }

    private String validateKey(TicketCreateRequest request) {
        String value = request == null || request.idempotencyKey() == null ? "" : request.idempotencyKey();
        if (!IDEMPOTENCY_KEY.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
        return value;
    }
}
