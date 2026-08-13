package com.yike.aftersaleagent.ticket;

public record TicketTaskRecord(long taskId, long ticketId, String status, int currentStep, int totalSteps) { }
