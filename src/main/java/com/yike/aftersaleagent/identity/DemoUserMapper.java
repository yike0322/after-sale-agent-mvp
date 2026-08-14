package com.yike.aftersaleagent.identity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DemoUserMapper {
    @Select("SELECT COUNT(*) FROM demo_user")
    long countDemoUsers();

    @Select("""
            SELECT du.id, du.display_name AS displayName, COALESCE(da.role, 'CUSTOMER') AS role
            FROM demo_user du
            LEFT JOIN demo_account da ON da.user_id = du.id
            WHERE du.id = #{id}
            """)
    CurrentDemoUser findById(@Param("id") long id);

    @Insert("INSERT INTO demo_user (id, display_name) VALUES (#{id}, #{displayName})")
    void insertUser(@Param("id") long id, @Param("displayName") String displayName);

    @Insert("""
            INSERT INTO order_info
                (user_id, order_no, product_id, product_name, product_type, order_amount,
                 order_status, paid_at, received_at)
            VALUES
                (#{userId}, #{orderNo}, #{productId}, #{productName}, #{productType}, #{orderAmount},
                 #{orderStatus}, #{paidAt}, #{receivedAt})
            """)
    void insertOrder(
            @Param("userId") long userId,
            @Param("orderNo") String orderNo,
            @Param("productId") String productId,
            @Param("productName") String productName,
            @Param("productType") String productType,
            @Param("orderAmount") BigDecimal orderAmount,
            @Param("orderStatus") String orderStatus,
            @Param("paidAt") LocalDateTime paidAt,
            @Param("receivedAt") LocalDateTime receivedAt);

    @Insert("""
            INSERT INTO coupon_info
                (user_id, coupon_code, coupon_name, threshold_amount, discount_amount, status)
            VALUES
                (#{userId}, #{couponCode}, #{couponName}, #{thresholdAmount}, #{discountAmount}, #{status})
            """)
    void insertCoupon(
            @Param("userId") long userId,
            @Param("couponCode") String couponCode,
            @Param("couponName") String couponName,
            @Param("thresholdAmount") BigDecimal thresholdAmount,
            @Param("discountAmount") BigDecimal discountAmount,
            @Param("status") String status);

    @Insert("""
            INSERT INTO coupon_info
                (user_id, coupon_code, coupon_name, threshold_amount, discount_amount, status, start_at, end_at)
            VALUES
                (#{userId}, #{couponCode}, #{couponName}, #{thresholdAmount}, #{discountAmount}, #{status},
                 #{startAt}, #{endAt})
            """)
    void insertCouponWithValidity(
            @Param("userId") long userId,
            @Param("couponCode") String couponCode,
            @Param("couponName") String couponName,
            @Param("thresholdAmount") BigDecimal thresholdAmount,
            @Param("discountAmount") BigDecimal discountAmount,
            @Param("status") String status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    @Insert("""
            INSERT INTO customer_ticket
                (id, user_id, ticket_type, priority, status, title, description, result_summary)
            VALUES
                (#{ticketId}, #{userId}, #{ticketType}, #{priority}, #{status}, #{title}, #{description}, #{resultSummary})
            """)
    void insertHistoricalTicket(
            @Param("ticketId") long ticketId,
            @Param("userId") long userId,
            @Param("ticketType") String ticketType,
            @Param("priority") String priority,
            @Param("status") String status,
            @Param("title") String title,
            @Param("description") String description,
            @Param("resultSummary") String resultSummary);

    @Insert("""
            INSERT INTO ticket_task (ticket_id, idempotency_key, status, current_step, total_steps)
            VALUES (#{ticketId}, #{idempotencyKey}, #{status}, #{currentStep}, #{totalSteps})
            """)
    void insertHistoricalTicketTask(
            @Param("ticketId") long ticketId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("status") String status,
            @Param("currentStep") int currentStep,
            @Param("totalSteps") int totalSteps);

    @Insert("""
            INSERT INTO agent_step_log
                (ticket_id, step_no, step_name, status, input_summary, output_summary, started_at, ended_at)
            VALUES
                (#{ticketId}, #{stepNo}, #{stepName}, #{status}, #{inputSummary}, #{outputSummary},
                 #{occurredAt}, #{occurredAt})
            """)
    void insertHistoricalStep(
            @Param("ticketId") long ticketId,
            @Param("stepNo") int stepNo,
            @Param("stepName") String stepName,
            @Param("status") String status,
            @Param("inputSummary") String inputSummary,
            @Param("outputSummary") String outputSummary,
            @Param("occurredAt") LocalDateTime occurredAt);

    @Insert("""
            INSERT INTO tool_call_log
                (ticket_id, tool_name, request_summary, response_summary, success, cost_time_ms, request_id)
            VALUES
                (#{ticketId}, #{toolName}, #{requestSummary}, #{responseSummary}, #{success}, #{costTimeMs}, #{requestId})
            """)
    void insertHistoricalToolCall(
            @Param("ticketId") long ticketId,
            @Param("toolName") String toolName,
            @Param("requestSummary") String requestSummary,
            @Param("responseSummary") String responseSummary,
            @Param("success") boolean success,
            @Param("costTimeMs") long costTimeMs,
            @Param("requestId") String requestId);
}
