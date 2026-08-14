package com.yike.aftersaleagent.identity;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DemoTokenService {
    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    private final DemoAccountMapper demoAccountMapper;
    private final Clock clock;
    private final byte[] secret;

    public DemoTokenService(
            DemoAccountMapper demoAccountMapper,
            Clock clock,
            @Value("${aftersale.demo-auth.hmac-secret}") String secret) {
        this.demoAccountMapper = demoAccountMapper;
        this.clock = clock;
        if (secret == null || secret.length() < 24) {
            throw new IllegalArgumentException("Demo auth HMAC secret must be at least 24 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(CurrentDemoUser user) {
        Instant expiresAt = clock.instant().plus(TOKEN_TTL);
        String payload = user.id() + "|" + user.role().name() + "|" + expiresAt.getEpochSecond();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + signature(encodedPayload);
    }

    public CurrentDemoUser verify(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(
                    signature(parts[0]).getBytes(StandardCharsets.US_ASCII),
                    parts[1].getBytes(StandardCharsets.US_ASCII))) {
                throw invalidToken();
            }
            String[] values = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
                    .split("\\|", -1);
            if (values.length != 3 || Instant.ofEpochSecond(Long.parseLong(values[2])).isBefore(clock.instant())) {
                throw invalidToken();
            }
            long userId = Long.parseLong(values[0]);
            DemoRole tokenRole = DemoRole.valueOf(values[1]);
            DemoAccount account = demoAccountMapper.findByUserId(userId);
            if (account == null || !account.enabled() || account.role() != tokenRole) {
                throw invalidToken();
            }
            return account.toCurrentUser();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    public Instant expiresAt() {
        return clock.instant().plus(TOKEN_TTL);
    }

    private String signature(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.US_ASCII)));
        } catch (InvalidKeyException | java.security.NoSuchAlgorithmException exception) {
            throw invalidToken();
        }
    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
    }
}
