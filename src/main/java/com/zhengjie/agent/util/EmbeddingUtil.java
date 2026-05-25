package com.zhengjie.agent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class EmbeddingUtil {
    private static final String OLLAMA_EMBED_URL = "http://localhost:11434/api/embeddings";
    private static final String MODEL = "bge-m3";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static float[] getEmbedding(String text) throws IOException, InterruptedException {
        // 用 Jackson 构建 JSON，自动处理特殊字符转义
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("model", MODEL);
        requestMap.put("prompt", text);
        String requestJson = mapper.writeValueAsString(requestMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_EMBED_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Embedding 请求失败: " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode embeddingNode = root.get("embedding");
        if (embeddingNode == null) {
            // 打印原始响应帮助调试
            String respBody = response.body();
            throw new IOException("Embedding 响应中没有 'embedding' 字段，原始响应前500字符: "
                    + respBody.substring(0, Math.min(500, respBody.length())));
        }

        System.out.println("[Embedding] 向量维度: " + embeddingNode.size());

        int nanCount = 0;
        float[] embedding = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            double val = embeddingNode.get(i).asDouble();
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                nanCount++;
                embedding[i] = 0.0f;  // NaN/Inf 替换为 0，避免 sqlite-vec 报错
            } else {
                embedding[i] = (float) val;
            }
        }
        if (nanCount > 0) {
            System.out.println("[Embedding] ⚠️ 检测到 " + nanCount + " 个 NaN/Inf，已替换为 0.0");
            System.out.println("[Embedding] 建议切换模型，如: nomic-embed-text");
        }
        return embedding;
    }

    /**
     * float[] 转 byte[]（小端序），供 sqlite-vec 使用。
     * Windows/x86 平台需要小端序，否则字节颠倒会导致 NaN。
     */
    public static byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
}
