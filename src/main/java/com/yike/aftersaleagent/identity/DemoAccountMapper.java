package com.yike.aftersaleagent.identity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DemoAccountMapper {
    @Select("SELECT COUNT(*) FROM demo_account")
    long countAccounts();

    @Select("""
            SELECT da.user_id AS userId, da.account, da.password_hash AS passwordHash, da.role,
                   da.enabled, du.display_name AS displayName
            FROM demo_account da
            JOIN demo_user du ON du.id = da.user_id
            WHERE da.account = #{account}
            """)
    @Options(timeout = 2)
    DemoAccount findByAccount(@Param("account") String account);

    @Select("""
            SELECT da.user_id AS userId, da.account, da.password_hash AS passwordHash, da.role,
                   da.enabled, du.display_name AS displayName
            FROM demo_account da
            JOIN demo_user du ON du.id = da.user_id
            WHERE da.user_id = #{userId}
            """)
    @Options(timeout = 2)
    DemoAccount findByUserId(@Param("userId") long userId);

    @Insert("""
            INSERT INTO demo_account (user_id, account, password_hash, role, enabled)
            VALUES (#{userId}, #{account}, #{passwordHash}, #{role}, #{enabled})
            """)
    @Options(timeout = 2)
    int insert(
            @Param("userId") long userId,
            @Param("account") String account,
            @Param("passwordHash") String passwordHash,
            @Param("role") String role,
            @Param("enabled") boolean enabled);
}
