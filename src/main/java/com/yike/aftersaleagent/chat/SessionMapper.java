package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.chat.domain.ChatMessage;
import com.yike.aftersaleagent.chat.domain.UserSession;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SessionMapper {
    @Insert("""
            INSERT INTO user_session (id, user_id, title, status, created_at, updated_at)
            VALUES (#{id}, #{userId}, #{title}, #{status}, #{createdAt}, #{updatedAt})
            """)
    void insertSession(UserSession session);

    @Select("SELECT COUNT(*) FROM user_session WHERE id = #{sessionId} AND user_id = #{userId}")
    int countOwnedSession(@Param("sessionId") String sessionId, @Param("userId") long userId);

    @Select("""
            SELECT m.id, m.session_id, m.user_id, m.role, m.content, m.intent,
                   m.created_at, m.updated_at
            FROM chat_message m
            INNER JOIN user_session s ON s.id = m.session_id
            WHERE s.id = #{sessionId} AND s.user_id = #{userId}
            ORDER BY m.created_at, m.id
            """)
    List<ChatMessage> findMessages(
            @Param("sessionId") String sessionId, @Param("userId") long userId);

    @Insert("""
            INSERT INTO chat_message
                (session_id, user_id, role, content, intent, created_at, updated_at)
            VALUES
                (#{sessionId}, #{userId}, #{role}, #{content}, #{intent}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(ChatMessage message);
}
