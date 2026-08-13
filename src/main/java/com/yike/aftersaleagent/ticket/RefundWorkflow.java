package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.function.Consumer;

public interface RefundWorkflow {
    RefundSubmission submit(AgentExecutionContext context, Consumer<String> statusSink);
}
