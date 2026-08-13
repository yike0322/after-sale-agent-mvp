package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.tool.AgentExecutionContext;

public interface KnowledgeAnswerService {
    ChatOutcome answer(AgentExecutionContext context);
}
