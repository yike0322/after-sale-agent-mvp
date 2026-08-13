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
}
