package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import java.util.concurrent.CompletableFuture;

public record RefundSubmission(
        long ticketId,
        boolean newlySubmitted,
        TicketTaskStatus currentStatus,
        CompletableFuture<ChatOutcome> completion) { }
