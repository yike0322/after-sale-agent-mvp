package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.api.AgentStepLogResponse;
import com.yike.aftersaleagent.ticket.api.TicketDetailResponse;
import com.yike.aftersaleagent.ticket.api.TicketTraceResponse;
import com.yike.aftersaleagent.ticket.api.ToolCallLogResponse;
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

    private TicketDetailRecord requireOwnedDetail(long userId, long ticketId) {
        TicketDetailRecord detail = ticketMapper.findOwnedDetail(userId, ticketId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND_OR_FORBIDDEN);
        }
        return detail;
    }
}
