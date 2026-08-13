package com.yike.aftersaleagent.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundIdempotencyKeyFactoryTest {
    private final RefundIdempotencyKeyFactory keyFactory = new RefundIdempotencyKeyFactory();

    @Test
    void createsStableBoundedAndUnambiguousKeysWithoutRawIdentity() {
        String first = keyFactory.create(10002L, "session-a", "O2001");

        assertThat(first).isEqualTo(keyFactory.create(10002L, "session-a", "O2001"));
        assertThat(first).startsWith("refund:v1:").hasSize(74)
                .doesNotContain("10002", "session-a", "O2001");
        assertThat(first).isNotEqualTo(keyFactory.create(10002L, "session-ab", "c"));
    }
}
