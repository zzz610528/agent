package com.zhengjie.agent.knowledge;

import com.zhengjie.agent.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeBase {
    // 根据用户问题中的关键词，检索最相关的知识片段（最多 3 条）
    public static List<String> retrieveRelevantChunks(String query) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT content FROM knowledge WHERE content LIKE ? LIMIT 3";
        // 提取查询中的词（极简：取前 5 个汉字作为关键词，实际可更智能）
        String keyword = "%" + (query.length() > 10 ? query.substring(0, 10) : query) + "%";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyword);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("content"));
            }
        }
        return results;
    }
}
