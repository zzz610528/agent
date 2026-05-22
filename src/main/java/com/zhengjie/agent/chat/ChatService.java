package com.zhengjie.agent.chat;

import com.zhengjie.agent.history.ConversationHistory;
import com.zhengjie.agent.knowledge.KnowledgeBase;

import java.util.List;
import java.util.Map;

public class ChatService {
    public static String chat(String sessionId, String userMessage) throws Exception {
        // 1. 检索相关知识
        List<String> knowledge = KnowledgeBase.retrieveRelevantChunks(userMessage);

        // 2. 获取最近对话历史（例如最近 5 条）
        List<Map<String, String>> history = ConversationHistory.getHistory(sessionId, 5);

        // 3. 保存用户消息到数据库
        ConversationHistory.saveMessage(sessionId, "user", userMessage);

        // 4. 构造 Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个乐于助人的AI助手。请基于以下参考资料和对话历史回答用户的最新问题。\n");
        prompt.append("如果参考资料中有相关信息，请优先使用；如果没有，就用自己的知识回答。\n\n");

        if (!knowledge.isEmpty()) {
            prompt.append("【参考资料】\n");
            for (String k : knowledge) {
                prompt.append("- ").append(k).append("\n");
            }
            prompt.append("\n");
        }

        if (!history.isEmpty()) {
            prompt.append("【对话历史】\n");
            for (Map<String, String> turn : history) {
                prompt.append(turn.get("role")).append(": ").append(turn.get("content")).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("用户最新问题: ").append(userMessage).append("\n");
        prompt.append("你的回答: ");

        // 5. 调用 Ollama
        String answer = OllamaClient.generate(prompt.toString());

        // 6. 保存助手回答到数据库
        ConversationHistory.saveMessage(sessionId, "assistant", answer);

        return answer;
    }
}
