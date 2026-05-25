package com.zhengjie.agent.knowledge;

import com.zhengjie.agent.util.EmbeddingUtil;
import com.zhengjie.agent.util.SQLiteUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeSearcher {

    // 检索最相似的 topK 个文本块
    public static List<String> search(String query, int topK) throws Exception {
        float[] queryVec = EmbeddingUtil.getEmbedding(query);
        byte[] queryBytes = EmbeddingUtil.floatArrayToBytes(queryVec);

        String sql = """
            SELECT m.content, m.source, v.distance
            FROM knowledge_meta m
            JOIN (
                SELECT rowid, distance
                FROM knowledge_vec
                WHERE embedding MATCH ?
                ORDER BY distance
                LIMIT ?
            ) v ON m.vec_rowid = v.rowid
            ORDER BY v.distance
        """;

        List<String> results = new ArrayList<>();
        try (Connection conn = SQLiteUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBytes(1, queryBytes);
            pstmt.setInt(2, topK);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String content = rs.getString("content");
                double distance = rs.getDouble("distance");
                // 可以格式化输出，或只返回 content
                results.add(content);
                System.out.printf("检索结果 (距离 %.4f): %s\n", distance, content.substring(0, Math.min(100, content.length())));
            }
        }
        return results;
    }
}
