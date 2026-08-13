package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultKnowledgeAnswerService implements KnowledgeAnswerService {
    private static final String INSUFFICIENT_MATERIALS_REPLY =
            "资料不足：当前知识库没有检索到可引用的售后规则，请补充订单或商品信息后重试。";
    private static final String FAQ_SYSTEM_INSTRUCTION = """
            你是售后规则说明助手。只能依据提供的资料作答，不要把建议说成承诺；
            如资料不足请明确说明。不要编造订单、优惠券、退款或引用来源。
            """;

    private final KnowledgeRetriever knowledgeRetriever;
    private final AiGateway aiGateway;

    public DefaultKnowledgeAnswerService(KnowledgeRetriever knowledgeRetriever, AiGateway aiGateway) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.aiGateway = aiGateway;
    }

    /** The public FAQ boundary always uses the trusted FAQ_QUERY -> AFTER_SALE taxonomy. */
    @Override
    public ChatOutcome answer(AgentExecutionContext context) {
        return answerForIntent(context, Intent.FAQ_QUERY);
    }

    ChatOutcome answerForIntent(AgentExecutionContext context, Intent intent) {
        if (intent != Intent.FAQ_QUERY) {
            return insufficientMaterials();
        }
        // NORMAL is a fixed demo classification, not user/model-provided metadata.
        List<SourceCitation> citations = deduplicate(knowledgeRetriever.retrieve(
                Intent.FAQ_QUERY, "NORMAL", context.userMessage(), 3));
        if (citations.isEmpty()) {
            return insufficientMaterials();
        }
        String reply = aiGateway.explain(FAQ_SYSTEM_INSTRUCTION, facts(citations));
        if (reply == null || reply.isBlank()) {
            reply = "已检索到相关资料，请结合引用规则补充订单信息后由人工确认。";
        }
        return new ChatOutcome(reply.strip(), null, "ANSWERED", citations);
    }

    private ChatOutcome insufficientMaterials() {
        return new ChatOutcome(INSUFFICIENT_MATERIALS_REPLY, null, "NO_EVIDENCE", List.of());
    }

    private List<SourceCitation> deduplicate(List<SourceCitation> citations) {
        return List.copyOf(new LinkedHashSet<>(citations == null ? List.of() : citations));
    }

    private String facts(List<SourceCitation> citations) {
        return citations.stream()
                .map(citation -> "来源：" + citation.sourceTitle() + "\n内容：" + citation.excerpt())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
