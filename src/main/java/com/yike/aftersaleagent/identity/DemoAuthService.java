package com.yike.aftersaleagent.identity;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.identity.api.LoginRequest;
import com.yike.aftersaleagent.identity.api.LoginResponse;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DemoAuthService {
    private final DemoAccountMapper demoAccountMapper;
    private final DemoTokenService demoTokenService;
    private final PasswordEncoder passwordEncoder;

    public DemoAuthService(
            DemoAccountMapper demoAccountMapper,
            DemoTokenService demoTokenService,
            PasswordEncoder passwordEncoder) {
        this.demoAccountMapper = demoAccountMapper;
        this.demoTokenService = demoTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String accountName = request == null || request.account() == null ? ""
                : request.account().strip().toLowerCase(Locale.ROOT);
        String password = request == null || request.password() == null ? "" : request.password();
        DemoAccount account = demoAccountMapper.findByAccount(accountName);
        if (account == null || !account.enabled() || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        CurrentDemoUser user = account.toCurrentUser();
        String token = demoTokenService.issue(user);
        Instant expiresAt = demoTokenService.expiresAt();
        return new LoginResponse(token, user.id(), user.displayName(), user.role().name(), expiresAt);
    }
}
