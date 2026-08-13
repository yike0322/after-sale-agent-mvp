package com.yike.aftersaleagent.agent;

import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class IntentRouter {
    private final AiGateway aiGateway;

    public IntentRouter(AiGateway aiGateway) {
        this.aiGateway = aiGateway;
    }

    public Intent route(AgentExecutionContext context) {
        return aiGateway.classifyIntent(context.userMessage());
    }
}
