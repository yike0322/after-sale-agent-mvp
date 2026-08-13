package com.yike.aftersaleagent.ticket;

public record TicketCreateRequest(String normalizedOrderNo, String idempotencyKey) { }
