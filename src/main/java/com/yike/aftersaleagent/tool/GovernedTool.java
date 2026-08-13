package com.yike.aftersaleagent.tool;

public interface GovernedTool<I, O> {
    String name();

    ToolRisk risk();

    O execute(AgentExecutionContext context, I input);
}
