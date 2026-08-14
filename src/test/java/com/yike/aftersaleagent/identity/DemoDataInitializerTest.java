package com.yike.aftersaleagent.identity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DemoDataInitializerTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-13T04:00:00Z"), ZoneOffset.UTC);

    @Autowired
    DemoUserMapper demoUserMapper;

    @Autowired
    DemoAccountMapper demoAccountMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void emptyDemoUserTableSeedsExactlyTheRequiredScenario() throws Exception {
        clearSeedTables();

        runInitializer();

        List<Long> userIds = jdbcTemplate.queryForList(
                "SELECT id FROM demo_user ORDER BY id", Long.class);
        assertThat(userIds).containsExactly(10001L, 10002L, 10003L);
        assertThat(count("demo_account")).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT account FROM demo_account WHERE user_id = 10003", String.class))
                .isEqualTo("supervisor_chen");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT role FROM demo_account WHERE user_id = 10003", String.class))
                .isEqualTo("SUPERVISOR");

        assertThat(count("order_info")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM order_info WHERE order_no = 'O1001'", Long.class))
                .isEqualTo(10001L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT order_amount FROM order_info WHERE order_no = 'O1001'", BigDecimal.class))
                .isEqualByComparingTo("80.00");

        assertThat(count("coupon_info")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM coupon_info WHERE coupon_code = 'C1001'", Long.class))
                .isEqualTo(10001L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT threshold_amount FROM coupon_info WHERE coupon_code = 'C1001'",
                BigDecimal.class))
                .isEqualByComparingTo("100.00");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM order_info WHERE order_no = 'O2001'", Long.class))
                .isEqualTo(10002L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT product_type FROM order_info WHERE order_no = 'O2001'", String.class))
                .isEqualTo("NORMAL");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT received_at FROM order_info WHERE order_no = 'O2001'", LocalDateTime.class))
                .isEqualTo(LocalDateTime.ofInstant(FIXED_CLOCK.instant(), ZoneOffset.UTC).minusDays(2));
    }

    @Test
    void nonemptyDemoUserTableSkipsAllSeedInserts() throws Exception {
        clearSeedTables();
        jdbcTemplate.update(
                "INSERT INTO demo_user (id, display_name) VALUES (?, ?)", 20000L, "Existing user");

        runInitializer();

        assertThat(jdbcTemplate.queryForList(
                "SELECT id FROM demo_user ORDER BY id", Long.class))
                .containsExactly(20000L);
        assertThat(count("order_info")).isZero();
        assertThat(count("coupon_info")).isZero();
        assertThat(count("demo_account")).isZero();
    }

    private void runInitializer() throws Exception {
        DemoDataInitializer initializer = new DemoDataInitializer(
                demoUserMapper, demoAccountMapper, passwordEncoder, FIXED_CLOCK);
        initializer.run(new DefaultApplicationArguments(new String[0]));
    }

    private void clearSeedTables() {
        jdbcTemplate.update("DELETE FROM demo_account");
        jdbcTemplate.update("DELETE FROM coupon_info");
        jdbcTemplate.update("DELETE FROM order_info");
        jdbcTemplate.update("DELETE FROM demo_user");
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
