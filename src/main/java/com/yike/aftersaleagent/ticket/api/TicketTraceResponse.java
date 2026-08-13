package com.yike.aftersaleagent.ticket.api;

import java.util.List;

public record TicketTraceResponse(
        long ticketId,
        List<AgentStepLogResponse> steps,
        List<ToolCallLogResponse> toolCalls) { }
