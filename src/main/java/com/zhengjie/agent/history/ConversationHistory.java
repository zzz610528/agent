package com.zhengjie.agent.history;

import com.zhengjie.agent.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ConversationHistory {
    // 获取某个 session 最近的 K 条记录（按时间升序）
    public static List<Map<String, String>> getHistory(String sessionId, int limit) throws SQLException {
        List<Map<String, String>> history = new ArrayList<>();
        String sql = "SELECT role, content FROM conversation WHERE session_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> turn = new HashMap<>();
                turn.put("role", rs.getString("role"));
                turn.put("content", rs.getString("content"));
                history.add(turn);
            }
        }
        Collections.reverse(history); // 转回正序
        return history;
    }

    // 保存一条消息
    public static void saveMessage(String sessionId, String role, String content) throws SQLException {
        String sql = "INSERT INTO conversation (session_id, role, content) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    // 获取所有会话列表（按最后活跃时间倒序）
    public static List<Map<String, Object>> getConversations() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT session_id, MAX(created_at) AS last_time, " +
                     "(SELECT content FROM conversation c2 WHERE c2.session_id = c1.session_id ORDER BY created_at ASC LIMIT 1) AS title " +
                     "FROM conversation c1 GROUP BY session_id ORDER BY last_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("sessionId", rs.getString("session_id"));
                item.put("lastTime", rs.getTimestamp("last_time").toString());
                String title = rs.getString("title");
                item.put("title", title != null && title.length() > 30 ? title.substring(0, 30) + "..." : title);
                list.add(item);
            }
        }
        return list;
    }

    // 删除某个会话的全部消息
    public static void deleteConversation(String sessionId) throws SQLException {
        String sql = "DELETE FROM conversation WHERE session_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        }
    }
}
