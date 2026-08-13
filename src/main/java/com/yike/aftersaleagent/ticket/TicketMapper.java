package com.yike.aftersaleagent.ticket;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

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
            @org.apache.ibatis.annotations.Param("userId") long userId,
            @org.apache.ibatis.annotations.Param("ticketId") long ticketId,
            @org.apache.ibatis.annotations.Param("status") String status,
            @org.apache.ibatis.annotations.Param("resultSummary") String resultSummary);
}
