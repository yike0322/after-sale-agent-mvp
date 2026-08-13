package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KnowledgeAnswerServiceTest {

    @Test
    void answersOnlyWithRetrievedEvidenceAndReturnsItsCitation() {
        KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
        AiGateway aiGateway = mock(AiGateway.class);
        SourceCitation evidence = new SourceCitation(
                "售后服务规则",
                "knowledge/after-sale-rule.md",
                "普通商品签收后 7 天内，未使用且无损坏时可申请退货。");
        given(retriever.retrieve(eq(com.yike.aftersaleagent.agent.Intent.FAQ_QUERY), eq("NORMAL"),
                any(), eq(3))).willReturn(List.of(evidence));
        given(aiGateway.explain(any(), any())).willReturn("可依据售后服务规则申请退货。");
        KnowledgeAnswerService service = new DefaultKnowledgeAnswerService(retriever, aiGateway);

        ChatOutcome outcome = service.answer(context("普通商品签收后多久可以退货？"));

        assertThat(outcome.reply()).isEqualTo("可依据售后服务规则申请退货。");
        assertThat(outcome.citations()).containsExactly(evidence);
        assertThat(outcome.citations()).extracting(SourceCitation::sourcePath)
                .containsExactly("knowledge/after-sale-rule.md");
        ArgumentCaptor<String> facts = ArgumentCaptor.forClass(String.class);
        verify(aiGateway).explain(any(), facts.capture());
        assertThat(facts.getValue()).contains("普通商品签收后 7 天内，未使用且无损坏时可申请退货。");
    }

    @Test
    void returnsInsufficientMaterialsWithoutCallingModelWhenNoEvidenceExists() {
        KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
        AiGateway aiGateway = mock(AiGateway.class);
        given(retriever.retrieve(eq(com.yike.aftersaleagent.agent.Intent.FAQ_QUERY), eq("NORMAL"),
                any(), eq(3))).willReturn(List.of());
        KnowledgeAnswerService service = new DefaultKnowledgeAnswerService(retriever, aiGateway);

        ChatOutcome outcome = service.answer(context("售后规则是什么？"));

        assertThat(outcome.reply()).contains("资料不足");
        assertThat(outcome.citations()).isEmpty();
        verify(aiGateway, never()).explain(any(), any());
    }

    @Test
    void deduplicatesOnlyActualRetrievedCitationsWithoutInventingProvenance() {
        KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
        AiGateway aiGateway = mock(AiGateway.class);
        SourceCitation evidence = new SourceCitation(
                "售后服务规则",
                "knowledge/after-sale-rule.md",
                "普通商品签收后 7 天内，未使用且无损坏时可申请退货。");
        SourceCitation secondEvidence = new SourceCitation(
                "常见问题",
                "knowledge/faq.md",
                "请准备订单号和优惠券编号，以便核对演示规则。");
        given(retriever.retrieve(eq(com.yike.aftersaleagent.agent.Intent.FAQ_QUERY), eq("NORMAL"),
                any(), eq(3))).willReturn(List.of(evidence, evidence, secondEvidence));
        given(aiGateway.explain(any(), any())).willReturn("根据资料说明处理。");
        KnowledgeAnswerService service = new DefaultKnowledgeAnswerService(retriever, aiGateway);

        ChatOutcome outcome = service.answer(context("请说明售后需要准备什么信息？"));

        assertThat(outcome.citations()).containsExactly(evidence, secondEvidence);
        assertThat(outcome.citations()).allSatisfy(citation ->
                assertThat(List.of(evidence, secondEvidence)).contains(citation));
    }

    @Test
    void returnsBoundedNoEvidenceOutcomeForAnUnexpectedNonFaqInvocation() {
        KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
        AiGateway aiGateway = mock(AiGateway.class);
        DefaultKnowledgeAnswerService service = new DefaultKnowledgeAnswerService(retriever, aiGateway);

        ChatOutcome outcome = service.answerForIntent(
                context("优惠券为什么不能使用？"), com.yike.aftersaleagent.agent.Intent.COUPON_ANALYSIS);

        assertThat(outcome.reply()).contains("资料不足");
        assertThat(outcome.citations()).isEmpty();
        verify(aiGateway, never()).explain(any(), any());
        verify(retriever, never()).retrieve(any(), any(), any(), anyInt());
    }

    private AgentExecutionContext context(String message) {
        return new AgentExecutionContext("request-1", 10001L, "session-1", message);
    }
}
