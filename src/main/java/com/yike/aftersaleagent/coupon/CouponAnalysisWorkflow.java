package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.function.Consumer;

public interface CouponAnalysisWorkflow {
    ChatOutcome execute(AgentExecutionContext context, Consumer<String> onStepStarted);

    default ChatOutcome execute(AgentExecutionContext context) {
        return execute(context, ignored -> { });
    }
}
