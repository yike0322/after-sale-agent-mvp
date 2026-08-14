package com.yike.aftersaleagent.ticket;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TicketMapper {
    @Insert("""
            INSERT INTO customer_ticket
                (user_id, order_id, ticket_type, priority, status, title, description)
            VALUES
                (#{userId}, NULL, #{ticketType}, #{priority}, #{status}, #{title}, #{description})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CustomerTicket ticket);

    @org.apache.ibatis.annotations.Update("""
            UPDATE customer_ticket
            SET status = #{status}, result_summary = #{resultSummary}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{ticketId} AND user_id = #{userId}
            """)
    int updateOwnedStatusAndSummary(
            @Param("userId") long userId,
            @Param("ticketId") long ticketId,
            @Param("status") String status,
            @Param("resultSummary") String resultSummary);

    @Select("""
            SELECT ct.id AS ticketId, ct.ticket_type AS ticketType, ct.status, ct.priority,
                   tt.current_step AS currentStep, tt.total_steps AS totalSteps,
                   ct.result_summary AS resultSummary
            FROM customer_ticket ct
            JOIN ticket_task tt ON tt.ticket_id = ct.id
            WHERE ct.id = #{ticketId} AND ct.user_id = #{userId}
            """)
    @Options(timeout = 2)
    TicketDetailRecord findOwnedDetail(@Param("userId") long userId, @Param("ticketId") long ticketId);

    @Select("""
            SELECT ct.id AS ticketId, ct.user_id AS userId, ct.ticket_type AS ticketType, ct.status,
                   ct.priority, tt.current_step AS currentStep, tt.total_steps AS totalSteps,
                   ct.updated_at AS updatedAt
            FROM customer_ticket ct
            JOIN ticket_task tt ON tt.ticket_id = ct.id
            WHERE ct.user_id = #{userId}
              AND (#{status} IS NULL OR ct.status = #{status})
            ORDER BY ct.updated_at DESC, ct.id DESC
            """)
    @Options(timeout = 2)
    List<TicketListItemRecord> listOwned(
            @Param("userId") long userId, @Param("status") String status);

    @Select("""
            SELECT ct.id AS ticketId, ct.user_id AS userId, ct.ticket_type AS ticketType, ct.status,
                   ct.priority, tt.current_step AS currentStep, tt.total_steps AS totalSteps,
                   ct.updated_at AS updatedAt
            FROM customer_ticket ct
            JOIN ticket_task tt ON tt.ticket_id = ct.id
            WHERE (#{status} IS NULL OR ct.status = #{status})
            ORDER BY ct.updated_at DESC, ct.id DESC
            """)
    @Options(timeout = 2)
    List<TicketListItemRecord> listAll(@Param("status") String status);
}
