package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;

public record TicketCreateResult(long ticketId, boolean newlySubmitted, TicketTaskStatus currentStatus) { }
