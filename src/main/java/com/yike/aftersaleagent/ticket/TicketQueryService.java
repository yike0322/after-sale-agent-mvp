package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.api.AgentStepLogResponse;
import com.yike.aftersaleagent.ticket.api.TicketDetailResponse;
import com.yike.aftersaleagent.ticket.api.TicketTraceResponse;
import com.yike.aftersaleagent.ticket.api.ToolCallLogResponse;
import com.yike.aftersaleagent.ticket.api.TicketListItemResponse;
import com.yike.aftersaleagent.ticket.api.TicketListResponse;
import com.yike.aftersaleagent.tool.ToolCallLogMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TicketQueryService {
    private final TicketMapper ticketMapper;
    private final AgentStepLogMapper agentStepLogMapper;
    private final ToolCallLogMapper toolCallLogMapper;

    public TicketQueryService(
            TicketMapper ticketMapper,
            AgentStepLogMapper agentStepLogMapper,
            ToolCallLogMapper toolCallLogMapper) {
        this.ticketMapper = ticketMapper;
        this.agentStepLogMapper = agentStepLogMapper;
        this.toolCallLogMapper = toolCallLogMapper;
    }

    public TicketDetailResponse getOwnedDetail(long userId, long ticketId) {
        TicketDetailRecord detail = requireOwnedDetail(userId, ticketId);
        return new TicketDetailResponse(detail.ticketId(), detail.ticketType(), detail.status(), detail.priority(),
                detail.currentStep(), detail.totalSteps(), detail.resultSummary());
    }

    public TicketTraceResponse getOwnedTrace(long userId, long ticketId) {
        requireOwnedDetail(userId, ticketId);
        List<AgentStepLogResponse> steps = agentStepLogMapper.findForOwnedTicket(userId, ticketId).stream()
                .map(step -> new AgentStepLogResponse(step.stepNo(), step.stepName(), step.status(),
                        step.inputSummary(), step.outputSummary(), step.errorCode(), step.startedAt(), step.endedAt()))
                .toList();
        List<ToolCallLogResponse> tools = toolCallLogMapper.findForOwnedTicket(userId, ticketId).stream()
                .map(tool -> new ToolCallLogResponse(tool.toolName(), tool.success(), tool.costTimeMs(),
                        tool.requestSummary(), tool.responseSummary(), tool.errorCode()))
                .toList();
        return new TicketTraceResponse(ticketId, steps, tools);
    }

    public TicketListResponse listOwnedTickets(long userId, String status) {
        return new TicketListResponse(ticketMapper.listOwned(userId, validateStatus(status)).stream()
                .map(ticket -> toListItem(ticket, null))
                .toList());
    }

    public TicketListResponse listAllTickets(String status) {
        return new TicketListResponse(ticketMapper.listAll(validateStatus(status)).stream()
                .map(ticket -> toListItem(ticket, ticket.userId()))
                .toList());
    }

    private TicketListItemResponse toListItem(TicketListItemRecord ticket, Long userId) {
        return new TicketListItemResponse(ticket.ticketId(), userId, ticket.ticketType(), ticket.status(),
                ticket.priority(), ticket.currentStep(), ticket.totalSteps(), ticket.updatedAt());
    }

    private String validateStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return com.yike.aftersaleagent.ticket.domain.TicketTaskStatus.valueOf(status.trim()).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private TicketDetailRecord requireOwnedDetail(long userId, long ticketId) {
        TicketDetailRecord detail = ticketMapper.findOwnedDetail(userId, ticketId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND_OR_FORBIDDEN);
        }
        return detail;
    }
}
