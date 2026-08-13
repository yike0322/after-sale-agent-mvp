package com.yike.aftersaleagent.order;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {
    @Select("""
            SELECT id, user_id AS userId, order_no AS orderNo, product_id AS productId,
                   product_name AS productName, product_type AS productType,
                   order_amount AS orderAmount, order_status AS orderStatus,
                   paid_at AS paidAt, received_at AS receivedAt
            FROM order_info
            WHERE user_id = #{userId} AND order_no = #{orderNo}
            """)
    @Options(timeout = 2)
    OrderInfo findOwnedByOrderNo(@Param("userId") long userId, @Param("orderNo") String orderNo);
}
