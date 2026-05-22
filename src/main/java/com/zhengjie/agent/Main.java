package com.zhengjie.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zhengjie.agent.chat.ChatService;
import com.zhengjie.agent.history.ConversationHistory;
import com.zhengjie.agent.util.SQLiteUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        ObjectMapper mapper = new ObjectMapper();
        server.createContext("/chat", new ChatHandler(mapper));
        server.createContext("/conversations", new ConversationsHandler(mapper));
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8081");
    }

    // ==================== /chat ====================
    static class ChatHandler implements HttpHandler {
        private final ObjectMapper mapper;

        ChatHandler(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode json = mapper.readTree(body);
            String sessionId = json.get("sessionId").asText();
            String message = json.get("message").asText();

            try {
                String answer = ChatService.chat(sessionId, message);
                String responseJson = mapper.writeValueAsString(Map.of("answer", answer));
                sendJson(exchange, 200, responseJson);
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    // ==================== /conversations ====================
    static class ConversationsHandler implements HttpHandler {
        private final ObjectMapper mapper;

        ConversationsHandler(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                if ("GET".equals(method)) {
                    handleList(exchange);
                } else if ("DELETE".equals(method)) {
                    handleDelete(exchange);
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        }

        // GET /conversations — 获取所有会话列表
        private void handleList(HttpExchange exchange) throws Exception {
            List<Map<String, Object>> conversations = ConversationHistory.getConversations();
            String json = mapper.writeValueAsString(Map.of("conversations", conversations));
            sendJson(exchange, 200, json);
        }

        // DELETE /conversations — 删除指定会话
        private void handleDelete(HttpExchange exchange) throws Exception {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode json = mapper.readTree(body);
            String sessionId = json.get("sessionId").asText();

            ConversationHistory.deleteConversation(sessionId);
            String responseJson = mapper.writeValueAsString(Map.of("success", true, "message", "会话已删除"));
            sendJson(exchange, 200, responseJson);
        }
    }

    // ==================== 工具方法 ====================
    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
