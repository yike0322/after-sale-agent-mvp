package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.common.api.ApiResponse;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.identity.DemoUserContext;
import com.yike.aftersaleagent.identity.SupervisorAuthorization;
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
    private final SupervisorAuthorization supervisorAuthorization;

    KnowledgeReindexController(
            KnowledgeIndexer knowledgeIndexer,
            DemoUserContext demoUserContext,
            SupervisorAuthorization supervisorAuthorization) {
        this.knowledgeIndexer = knowledgeIndexer;
        this.demoUserContext = demoUserContext;
        this.supervisorAuthorization = supervisorAuthorization;
    }

    @PostMapping("/reindex")
    ApiResponse<KnowledgeIndexResult> reindex() {
        supervisorAuthorization.requireSupervisor(demoUserContext.requireCurrentUser());
        return ApiResponse.success(
                knowledgeIndexer.reindex(), MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }
}
