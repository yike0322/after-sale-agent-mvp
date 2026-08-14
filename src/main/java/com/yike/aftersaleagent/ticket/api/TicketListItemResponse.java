package com.yike.aftersaleagent.ticket.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketListItemResponse(
        long ticketId,
        Long userId,
        String ticketType,
        String status,
        String priority,
        int currentStep,
        int totalSteps,
        LocalDateTime updatedAt) { }
