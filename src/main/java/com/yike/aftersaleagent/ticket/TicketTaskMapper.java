package com.yike.aftersaleagent.ticket;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketTaskMapper {
    @Select("""
            SELECT tt.id AS taskId, tt.ticket_id AS ticketId, tt.status,
                   tt.current_step AS currentStep, tt.total_steps AS totalSteps
            FROM ticket_task tt
            JOIN customer_ticket ct ON ct.id = tt.ticket_id
            WHERE ct.user_id = #{userId} AND tt.idempotency_key = #{idempotencyKey}
            """)
    @Options(timeout = 2)
    TicketTaskRecord findOwnedByIdempotency(
            @Param("userId") long userId, @Param("idempotencyKey") String idempotencyKey);

    @Insert("""
            INSERT INTO ticket_task
                (ticket_id, idempotency_key, status, current_step, total_steps)
            VALUES
                (#{ticketId}, #{idempotencyKey}, #{status}, #{currentStep}, #{totalSteps})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TicketTask task);
}
