package com.zhengjie.agent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EmbeddingUtil {
    private static final String OLLAMA_EMBED_URL = "http://localhost:11434/api/embeddings";
    private static final String MODEL = "bge-small-zh"; // 或 all-minilm:latest
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static float[] getEmbedding(String text) throws IOException, InterruptedException {
        String requestJson = String.format("{\"model\":\"%s\", \"prompt\":\"%s\"}",
                MODEL, text.replace("\"", "\\\""));
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
        float[] embedding = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            embedding[i] = (float) embeddingNode.get(i).asDouble();
        }
        return embedding;
    }
}
