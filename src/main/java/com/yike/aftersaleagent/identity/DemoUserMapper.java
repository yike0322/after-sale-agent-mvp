package com.yike.aftersaleagent.identity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DemoUserMapper {
    @Select("SELECT COUNT(*) FROM demo_user")
    long countDemoUsers();

    @Select("""
            SELECT du.id, du.display_name AS displayName, COALESCE(da.role, 'CUSTOMER') AS role
            FROM demo_user du
            LEFT JOIN demo_account da ON da.user_id = du.id
            WHERE du.id = #{id}
            """)
    CurrentDemoUser findById(@Param("id") long id);

    @Insert("INSERT INTO demo_user (id, display_name) VALUES (#{id}, #{displayName})")
    void insertUser(@Param("id") long id, @Param("displayName") String displayName);

    @Insert("""
            INSERT INTO order_info
                (user_id, order_no, product_id, product_name, product_type, order_amount,
                 order_status, paid_at, received_at)
            VALUES
                (#{userId}, #{orderNo}, #{productId}, #{productName}, #{productType}, #{orderAmount},
                 #{orderStatus}, #{paidAt}, #{receivedAt})
            """)
    void insertOrder(
            @Param("userId") long userId,
            @Param("orderNo") String orderNo,
            @Param("productId") String productId,
            @Param("productName") String productName,
            @Param("productType") String productType,
            @Param("orderAmount") BigDecimal orderAmount,
            @Param("orderStatus") String orderStatus,
            @Param("paidAt") LocalDateTime paidAt,
            @Param("receivedAt") LocalDateTime receivedAt);

    @Insert("""
            INSERT INTO coupon_info
                (user_id, coupon_code, coupon_name, threshold_amount, discount_amount, status)
            VALUES
                (#{userId}, #{couponCode}, #{couponName}, #{thresholdAmount}, #{discountAmount}, #{status})
            """)
    void insertCoupon(
            @Param("userId") long userId,
            @Param("couponCode") String couponCode,
            @Param("couponName") String couponName,
            @Param("thresholdAmount") BigDecimal thresholdAmount,
            @Param("discountAmount") BigDecimal discountAmount,
            @Param("status") String status);
}
