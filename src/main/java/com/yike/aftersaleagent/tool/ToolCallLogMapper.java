package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.ticket.ToolCallLogRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.util.List;

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

    @Select("""
            SELECT tcl.tool_name AS toolName, tcl.success, tcl.cost_time_ms AS costTimeMs,
                   tcl.request_summary AS requestSummary, tcl.response_summary AS responseSummary,
                   tcl.error_message AS errorCode
            FROM tool_call_log tcl
            JOIN customer_ticket ct ON ct.id = tcl.ticket_id
            WHERE ct.id = #{ticketId} AND ct.user_id = #{userId}
            ORDER BY tcl.created_at ASC, tcl.id ASC
            """)
    @org.apache.ibatis.annotations.Options(timeout = 2)
    List<ToolCallLogRecord> findForOwnedTicket(@Param("userId") long userId, @Param("ticketId") long ticketId);

    @Select("""
            SELECT tool_name AS toolName, success, cost_time_ms AS costTimeMs,
                   request_summary AS requestSummary, response_summary AS responseSummary,
                   error_message AS errorCode
            FROM tool_call_log
            WHERE ticket_id = #{ticketId}
            ORDER BY created_at ASC, id ASC
            """)
    @org.apache.ibatis.annotations.Options(timeout = 2)
    List<ToolCallLogRecord> findForTicket(@Param("ticketId") long ticketId);
}
