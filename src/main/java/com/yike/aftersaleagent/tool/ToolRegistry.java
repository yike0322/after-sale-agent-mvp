package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {
    private static final Map<Intent, Set<String>> ALLOWED_TOOLS = allowedTools();

    private final Map<String, GovernedTool<?, ?>> registeredTools;

    public ToolRegistry(List<GovernedTool<?, ?>> tools) {
        Map<String, GovernedTool<?, ?>> registrations = new HashMap<>();
        for (GovernedTool<?, ?> tool : tools) {
            String toolName = tool.name();
            if (toolName == null || toolName.isBlank()) {
                throw new IllegalArgumentException("Governed tools require a non-blank name");
            }
            if (registrations.putIfAbsent(toolName, tool) != null) {
                throw new IllegalArgumentException("Duplicate governed tool name: " + toolName);
            }
        }
        this.registeredTools = Map.copyOf(registrations);
    }

    public void requireAllowed(Intent intent, String toolName) {
        if (intent == null || toolName == null || !ALLOWED_TOOLS.getOrDefault(intent, Set.of()).contains(toolName)) {
            throw new BusinessException(ErrorCode.TOOL_NOT_ALLOWED);
        }
    }

    public <I, O> O execute(
            Intent intent, GovernedTool<I, O> tool, AgentExecutionContext context, I input) {
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_ALLOWED);
        }
        requireAllowed(intent, tool.name());
        if (registeredTools.get(tool.name()) != tool) {
            throw new BusinessException(ErrorCode.TOOL_NOT_ALLOWED);
        }
        return tool.execute(context, input);
    }

    private static Map<Intent, Set<String>> allowedTools() {
        Map<Intent, Set<String>> tools = new EnumMap<>(Intent.class);
        tools.put(Intent.FAQ_QUERY, Set.of());
        tools.put(Intent.COUPON_ANALYSIS, Set.of("orderQuery", "couponQuery"));
        tools.put(Intent.REFUND_ELIGIBILITY, Set.of("orderQuery", "afterSaleRuleQuery", "ticketCreate"));
        tools.put(Intent.UNSUPPORTED, Set.of());
        return Map.copyOf(tools);
    }
}
