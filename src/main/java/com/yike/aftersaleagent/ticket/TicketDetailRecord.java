package com.yike.aftersaleagent.ticket;

public record TicketDetailRecord(
        long ticketId,
        String ticketType,
        String status,
        String priority,
        int currentStep,
        int totalSteps,
        String resultSummary) { }
