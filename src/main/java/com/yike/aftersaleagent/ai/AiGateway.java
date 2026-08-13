package com.yike.aftersaleagent.ai;

import com.yike.aftersaleagent.agent.Intent;

public interface AiGateway {
    Intent classifyIntent(String message);
    String explain(String systemInstruction, String facts);
}
