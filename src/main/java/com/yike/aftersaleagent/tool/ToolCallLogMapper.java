package com.yike.aftersaleagent.tool;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ToolCallLogMapper {
    @Insert("""
            INSERT INTO tool_call_log
                (ticket_id, tool_name, request_summary, response_summary, success,
                 cost_time_ms, error_message, request_id)
            VALUES
                (NULL, #{toolName}, #{requestSummary}, #{responseSummary}, #{success},
                 #{costTimeMs}, #{errorMessage}, #{requestId})
            """)
    void insert(
            @Param("toolName") String toolName,
            @Param("requestSummary") String requestSummary,
            @Param("responseSummary") String responseSummary,
            @Param("success") boolean success,
            @Param("costTimeMs") long costTimeMs,
            @Param("errorMessage") String errorMessage,
            @Param("requestId") String requestId);

    @Update("""
            UPDATE tool_call_log
            SET ticket_id = #{ticketId}
            WHERE ticket_id IS NULL
              AND request_id = #{requestId}
              AND tool_name IN ('orderQuery', 'afterSaleRuleQuery', 'ticketCreate')
              AND EXISTS (
                  SELECT 1 FROM customer_ticket ct
                  WHERE ct.id = #{ticketId} AND ct.user_id = #{userId}
              )
            """)
    @org.apache.ibatis.annotations.Options(timeout = 2)
    int linkRefundAuditsToOwnedTicket(
            @Param("userId") long userId,
            @Param("ticketId") long ticketId,
            @Param("requestId") String requestId);
}
