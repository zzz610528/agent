package com.zhengjie.agent.knowledge;

import com.zhengjie.agent.util.EmbeddingUtil;
import com.zhengjie.agent.util.SQLiteUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeVectorStore {

    // 将文本块存入知识库
    public static void storeChunk(String content, String source) throws Exception {
        // 1. 生成向量
        float[] embedding = EmbeddingUtil.getEmbedding(content);

        // 2. 插入虚拟表 vec0，获得 rowid
        String insertVecSQL = "INSERT INTO knowledge_vec (embedding) VALUES (?)";
        long vecRowId;
        try (Connection conn = SQLiteUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertVecSQL, Statement.RETURN_GENERATED_KEYS)) {
            // 将 float[] 转换为 byte[] (sqlite-vec 期望 BLOB 格式，但也可以通过 JSON? 查看文档)
            // sqlite-vec 的插入方式：直接传入 BLOB 或 字符串? 根据官方文档，需要先将 float 数组转为字节数组
            byte[] vecBytes = floatArrayToByteArray(embedding);
            pstmt.setBytes(1, vecBytes);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                vecRowId = rs.getLong(1);
            } else {
                throw new SQLException("插入虚拟表失败，未获得 rowid");
            }
        }

        // 3. 插入元数据表
        String insertMetaSQL = "INSERT INTO knowledge_meta (content, source, vec_rowid) VALUES (?, ?, ?)";
        try (Connection conn = SQLiteUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertMetaSQL)) {
            pstmt.setString(1, content);
            pstmt.setString(2, source);
            pstmt.setLong(3, vecRowId);
            pstmt.executeUpdate();
        }
    }

    // 辅助：float[] 转 byte[]
    private static byte[] floatArrayToByteArray(float[] floats) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    // 如果需要存储分块后的多个片段
    public static void storeDocument(String text, String source, int chunkSize, int overlap) {
        List<String> chunks = splitText(text, chunkSize, overlap);
        for (String chunk : chunks) {
            try {
                storeChunk(chunk, source);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 简单分块实现（或用 LangChain 的文本分割器）
    private static List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // 尽量在句子结束处切断
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > start) end = lastPeriod + 1;
            }
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }
}
