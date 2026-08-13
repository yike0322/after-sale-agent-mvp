package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.knowledge.KnowledgeRetriever;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolAuditRequest;
import com.yike.aftersaleagent.tool.ToolAuditService;
import com.yike.aftersaleagent.tool.ToolOperationExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AfterSaleRuleQueryToolTest {
    private static final AgentExecutionContext CONTEXT = new AgentExecutionContext(
            "req-refund-rule-1", 10002L, "session-1", "DO_NOT_LOG O2001");

    @Test
    void retrievesOnlyActualEvidenceWithATrustedRefundIntentAndSafeAudit() {
        KnowledgeRetriever retriever = mock(KnowledgeRetriever.class);
        ToolAuditService auditService = mock(ToolAuditService.class);
        ToolOperationExecutor executor = mock(ToolOperationExecutor.class);
        SourceCitation citation = new SourceCitation(
                "退款资格说明", "knowledge/refund-rule.md", "仅提供资格建议，最终进入人工审核。");
        given(executor.execute(any())).willAnswer(invocation -> invocation.<java.util.concurrent.Callable<?>>getArgument(0).call());
        given(retriever.retrieve(Intent.REFUND_ELIGIBILITY, "NORMAL", "普通商品退款资格规则", 3))
                .willReturn(List.of(citation, citation));
        AfterSaleRuleQueryTool tool = new AfterSaleRuleQueryTool(retriever, auditService, executor);

        List<SourceCitation> evidence = tool.execute(CONTEXT, new AfterSaleRuleQuery("NORMAL"));

        assertThat(evidence).containsExactly(citation);
        verify(retriever).retrieve(Intent.REFUND_ELIGIBILITY, "NORMAL", "普通商品退款资格规则", 3);
        org.mockito.ArgumentCaptor<ToolAuditRequest> request = org.mockito.ArgumentCaptor.forClass(ToolAuditRequest.class);
        verify(auditService).recordSuccess(eq("afterSaleRuleQuery"), request.capture(), any(Long.class));
        assertThat(request.getValue().requestSummary()).isEqualTo("productType=NORMAL");
        assertThat(request.getValue().responseSummary()).isEqualTo("evidenceCount=1");
        assertThat(request.getValue().requestSummary() + request.getValue().responseSummary())
                .doesNotContain("DO_NOT_LOG", CONTEXT.userMessage(), CONTEXT.sessionId(), "10002");
    }
}
