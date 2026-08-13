package com.yike.aftersaleagent.ticket.domain;

public record RefundDecision(
        boolean eligible,
        boolean requiresHumanReview,
        String reasonCode,
        String reasonText,
        TicketTaskStatus ticketStatus) { }
