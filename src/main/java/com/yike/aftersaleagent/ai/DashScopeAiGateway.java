package com.yike.aftersaleagent.ai;

import com.yike.aftersaleagent.agent.Intent;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dashscope")
public class DashScopeAiGateway implements AiGateway {
    private static final String CLASSIFICATION_INSTRUCTION = """
            Classify the user message as exactly one of FAQ_QUERY, COUPON_ANALYSIS,
            REFUND_ELIGIBILITY, or UNSUPPORTED. Return only that enum name.
            """;

    private final ChatClient chatClient;

    public DashScopeAiGateway(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Intent classifyIntent(String message) {
        String response = chatClient.prompt()
                .system(CLASSIFICATION_INSTRUCTION)
                .user(message)
                .call()
                .content();
        return parseAllowedIntent(response);
    }

    @Override
    public String explain(String systemInstruction, String facts) {
        return chatClient.prompt()
                .system(systemInstruction)
                .user(facts)
                .call()
                .content();
    }

    private Intent parseAllowedIntent(String response) {
        if (response == null) {
            return Intent.UNSUPPORTED;
        }
        String normalized = response.strip().toUpperCase(Locale.ROOT);
        try {
            return Intent.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return Intent.UNSUPPORTED;
        }
    }
}
