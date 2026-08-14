package com.yike.aftersaleagent.identity;

import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.identity.api.LoginRequest;
import com.yike.aftersaleagent.identity.api.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DemoAuthServiceTest {
    @Autowired DemoAuthService authService;
    @Autowired DemoTokenService tokenService;

    @Test
    void validCustomerPasswordIssuesVerifiableToken() {
        LoginResponse login = authService.login(new LoginRequest("buyer_li", "Buyer#2026"));

        assertThat(login.role()).isEqualTo("CUSTOMER");
        assertThat(tokenService.verify(login.token()).displayName()).isEqualTo("Demo User 10001");
    }

    @Test
    void invalidPasswordUsesStableError() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("buyer_li", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode().getCode())
                .isEqualTo("AUTH_INVALID_CREDENTIALS");
    }
}
