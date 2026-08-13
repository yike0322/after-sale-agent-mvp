package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.knowledge.KnowledgeRetriever;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.GovernedTool;
import com.yike.aftersaleagent.tool.ToolAuditRequest;
import com.yike.aftersaleagent.tool.ToolAuditService;
import com.yike.aftersaleagent.tool.ToolOperationExecutor;
import com.yike.aftersaleagent.tool.ToolRisk;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AfterSaleRuleQueryTool implements GovernedTool<AfterSaleRuleQuery, List<SourceCitation>> {
    private static final String FIXED_REFUND_RULE_QUERY = "普通商品退款资格规则";
    private static final Pattern PRODUCT_TYPE = Pattern.compile("[A-Z_]{1,32}");

    private final KnowledgeRetriever knowledgeRetriever;
    private final ToolAuditService toolAuditService;
    private final ToolOperationExecutor toolOperationExecutor;

    public AfterSaleRuleQueryTool(
            KnowledgeRetriever knowledgeRetriever,
            ToolAuditService toolAuditService,
            ToolOperationExecutor toolOperationExecutor) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.toolAuditService = toolAuditService;
        this.toolOperationExecutor = toolOperationExecutor;
    }

    @Override
    public String name() { return "afterSaleRuleQuery"; }

    @Override
    public ToolRisk risk() { return ToolRisk.READ_ONLY; }

    @Override
    public List<SourceCitation> execute(AgentExecutionContext context, AfterSaleRuleQuery input) {
        long startedAt = System.nanoTime();
        String productType = normalizeProductType(input);
        try {
            List<SourceCitation> evidence = toolOperationExecutor.execute(() -> knowledgeRetriever.retrieve(
                    Intent.REFUND_ELIGIBILITY, productType, FIXED_REFUND_RULE_QUERY, 3));
            List<SourceCitation> safeEvidence = List.copyOf(new LinkedHashSet<>(evidence == null ? List.of() : evidence));
            toolAuditService.recordSuccess(name(), ToolAuditRequest.success(
                    context.requestId(), "productType=" + productType, "evidenceCount=" + safeEvidence.size()), elapsed(startedAt));
            return safeEvidence;
        } catch (BusinessException exception) {
            return failure(context, productType, startedAt, exception);
        } catch (RuntimeException exception) {
            return failure(context, productType, startedAt, new BusinessException(ErrorCode.TOOL_EXECUTION_FAILED));
        }
    }

    private List<SourceCitation> failure(
            AgentExecutionContext context, String productType, long startedAt, BusinessException exception) {
        try {
            toolAuditService.recordFailure(name(), ToolAuditRequest.failure(
                    context.requestId(), "productType=" + productType, exception.getErrorCode()), elapsed(startedAt));
        } catch (RuntimeException auditException) {
            throw new BusinessException(ErrorCode.TOOL_AUDIT_FAILED);
        }
        throw exception;
    }

    private String normalizeProductType(AfterSaleRuleQuery input) {
        String productType = input == null || input.productType() == null ? "" : input.productType().strip().toUpperCase(Locale.ROOT);
        if (!PRODUCT_TYPE.matcher(productType).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return productType;
    }

    private long elapsed(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }
}
