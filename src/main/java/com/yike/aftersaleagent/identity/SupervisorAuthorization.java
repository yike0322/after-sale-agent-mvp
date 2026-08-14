package com.yike.aftersaleagent.identity;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class SupervisorAuthorization {
    public void requireSupervisor(CurrentDemoUser user) {
        if (user.role() != DemoRole.SUPERVISOR) {
            throw new BusinessException(ErrorCode.AUTH_ROLE_FORBIDDEN);
        }
    }
}
