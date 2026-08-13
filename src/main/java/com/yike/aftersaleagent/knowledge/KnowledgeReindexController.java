package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.common.api.ApiResponse;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.identity.DemoUserContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local & dashscope")
@RequestMapping("/api/knowledge")
class KnowledgeReindexController {
    private final KnowledgeIndexer knowledgeIndexer;
    private final DemoUserContext demoUserContext;

    KnowledgeReindexController(KnowledgeIndexer knowledgeIndexer, DemoUserContext demoUserContext) {
        this.knowledgeIndexer = knowledgeIndexer;
        this.demoUserContext = demoUserContext;
    }

    @PostMapping("/reindex")
    ApiResponse<KnowledgeIndexResult> reindex() {
        demoUserContext.requireCurrentUser();
        return ApiResponse.success(
                knowledgeIndexer.reindex(), MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }
}
