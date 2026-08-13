package com.yike.aftersaleagent.coupon;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CouponMapper {
    @Select("""
            SELECT id, user_id AS userId, coupon_code AS couponCode, coupon_name AS couponName,
                   threshold_amount AS thresholdAmount, discount_amount AS discountAmount,
                   status, start_at AS startAt, end_at AS endAt
            FROM coupon_info
            WHERE user_id = #{userId} AND coupon_code = #{couponCode}
            """)
    @Options(timeout = 2)
    CouponInfo findOwnedByCouponCode(@Param("userId") long userId, @Param("couponCode") String couponCode);
}
