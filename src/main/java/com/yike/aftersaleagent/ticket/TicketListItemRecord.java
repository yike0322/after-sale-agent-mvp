package com.yike.aftersaleagent.ticket;

import java.time.LocalDateTime;

public record TicketListItemRecord(
        long ticketId,
        long userId,
        String ticketType,
        String status,
        String priority,
        int currentStep,
        int totalSteps,
        LocalDateTime updatedAt) { }
