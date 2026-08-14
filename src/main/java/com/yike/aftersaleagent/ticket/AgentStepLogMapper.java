package com.yike.aftersaleagent.ticket;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

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

    @Select("""
            SELECT asl.step_no AS stepNo, asl.step_name AS stepName, asl.status,
                   asl.input_summary AS inputSummary, asl.output_summary AS outputSummary,
                   asl.error_message AS errorCode, asl.started_at AS startedAt, asl.ended_at AS endedAt
            FROM agent_step_log asl
            JOIN customer_ticket ct ON ct.id = asl.ticket_id
            WHERE ct.id = #{ticketId} AND ct.user_id = #{userId}
            ORDER BY asl.step_no ASC, asl.id ASC
            """)
    @Options(timeout = 2)
    List<AgentStepLogRecord> findForOwnedTicket(@Param("userId") long userId, @Param("ticketId") long ticketId);

    @Select("""
            SELECT step_no AS stepNo, step_name AS stepName, status,
                   input_summary AS inputSummary, output_summary AS outputSummary,
                   error_message AS errorCode, started_at AS startedAt, ended_at AS endedAt
            FROM agent_step_log
            WHERE ticket_id = #{ticketId}
            ORDER BY step_no ASC, id ASC
            """)
    @Options(timeout = 2)
    List<AgentStepLogRecord> findForTicket(@Param("ticketId") long ticketId);
}
