package com.yike.aftersaleagent.tool;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
