package com.yike.aftersaleagent.knowledge;

import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.identity.DemoRole;
import com.yike.aftersaleagent.identity.DemoUserContext;
import com.yike.aftersaleagent.identity.SupervisorAuthorization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgeReindexRoleTest {
    @Test
    void customerCannotStartKnowledgeReindex() {
        KnowledgeIndexer knowledgeIndexer = mock(KnowledgeIndexer.class);
        DemoUserContext userContext = mock(DemoUserContext.class);
        when(userContext.requireCurrentUser())
                .thenReturn(new CurrentDemoUser(10001L, "Buyer Li", DemoRole.CUSTOMER));
        KnowledgeReindexController controller = new KnowledgeReindexController(
                knowledgeIndexer, userContext, new SupervisorAuthorization());

        assertThatThrownBy(controller::reindex)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode().getCode())
                .isEqualTo("AUTH_ROLE_FORBIDDEN");
        verifyNoInteractions(knowledgeIndexer);
    }
}
