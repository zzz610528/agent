package com.zhengjie.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OllamaClient - Ollama大模型API客户端工具类
 * <p>
 * 通过HTTP请求调用本地Ollama服务，与DeepSeek-R1模型进行交互，
 * 发送提示词并获取模型生成的文本响应。
 * </p>
 *
 * @author zhengjie
 * @version 1.0
 */
public class OllamaClient {

    /** Ollama服务API地址 */
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    /** 默认使用的模型名称 */
    private static final String MODEL = "deepseek-r1:7b";

    /** OkHttp客户端实例（超时 2 分钟，大模型推理需要较长时间） */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /** Jackson ObjectMapper实例，用于JSON序列化与反序列化 */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用Ollama生成接口，向模型发送提示词并返回生成的文本
     *
     * @param prompt 输入的提示词内容
     * @return 模型生成的文本响应
     * @throws IOException 当网络请求或JSON解析失败时抛出
     */
    public static String generate(String prompt) throws IOException {
        // 构建请求参数Map
        Map<String, Object> req = new HashMap<>();
        req.put("model", MODEL);
        req.put("prompt", prompt);
        req.put("stream", false);

        // 将请求参数序列化为JSON字符串
        String json = mapper.writeValueAsString(req);

        // 构建HTTP POST请求
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(OLLAMA_URL).post(body).build();

        // 执行请求并解析响应
        try (Response response = client.newCall(request).execute()) {
            String respBody = null;
            if (response.body() != null) {
                respBody = response.body().string();
            }
            // 从响应JSON中提取"response"字段
            JsonNode node = mapper.readTree(respBody);
            return node.get("response").asText();
        }
    }
}
