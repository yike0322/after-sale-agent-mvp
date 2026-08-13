package com.yike.aftersaleagent.ticket;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentStepLogMapper {
    @Insert("""
            INSERT INTO agent_step_log
                (ticket_id, step_no, step_name, status, input_summary, output_summary,
                 error_message, started_at, ended_at)
            VALUES
                (#{ticketId}, #{stepNo}, #{stepName}, #{status}, #{inputSummary}, #{outputSummary},
                 #{errorCode}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    @Options(timeout = 2)
    int insertCompleted(
            @Param("ticketId") long ticketId,
            @Param("stepNo") int stepNo,
            @Param("stepName") String stepName,
            @Param("status") String status,
            @Param("inputSummary") String inputSummary,
            @Param("outputSummary") String outputSummary,
            @Param("errorCode") String errorCode);
}
