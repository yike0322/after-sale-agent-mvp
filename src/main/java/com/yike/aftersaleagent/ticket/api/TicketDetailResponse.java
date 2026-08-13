package com.yike.aftersaleagent.ticket.api;

public record TicketDetailResponse(
        long ticketId,
        String ticketType,
        String status,
        String priority,
        int currentStep,
        int totalSteps,
        String resultSummary) { }
