package com.yike.aftersaleagent.chat.api;

import java.util.List;

public record ChatOutcome(
        String reply,
        Long ticketId,
        String taskStatus,
        List<SourceCitation> citations) { }
