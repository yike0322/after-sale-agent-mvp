package com.yike.aftersaleagent.ticket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RefundIdempotencyKeyFactory {
    public String create(long userId, String sessionId, String normalizedOrderNo) {
        String canonical = "refund-canonical-v1|u:" + userId + "|s:" + field(sessionId)
                + "|o:" + field(normalizedOrderNo);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return "refund:v1:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String field(String value) {
        String safeValue = Objects.requireNonNull(value, "idempotency key field");
        return safeValue.getBytes(StandardCharsets.UTF_8).length + ":" + safeValue;
    }
}
