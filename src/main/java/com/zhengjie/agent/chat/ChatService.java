package com.zhengjie.agent.chat;

import com.zhengjie.agent.history.ConversationHistory;
import com.zhengjie.agent.knowledge.KnowledgeBase;
import com.zhengjie.agent.knowledge.KnowledgeSearcher;
import com.zhengjie.agent.knowledge.KnowledgeVectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatService {

    // 自动存入知识库的最小回答长度（太短没价值）
    private static final int MIN_KNOWLEDGE_LENGTH = 100;
    // 向量检索返回的最大条数
    private static final int SEARCH_TOP_K = 3;
    // 是否自动存储问答到知识库
    private static volatile boolean autoStoreEnabled = true;

    /**
     * 核心对话方法：检索知识 → 加载历史 → 生成回答 → 存储知识
     */
    public static String chat(String sessionId, String userMessage) throws Exception {
        // 1. 检索相关知识（向量优先，关键词降级）
        List<String> knowledge = retrieveKnowledge(userMessage);

        // 2. 获取最近对话历史
        List<Map<String, String>> history = ConversationHistory.getHistory(sessionId, 5);

        // 3. 保存用户消息
        ConversationHistory.saveMessage(sessionId, "user", userMessage);

        // 4. 构造 Prompt
        StringBuilder prompt = buildPrompt(knowledge, history, userMessage);

        // 5. 调用 Ollama 生成回答
        String answer = OllamaClient.generate(prompt.toString());

        // 6. 保存助手回答
        ConversationHistory.saveMessage(sessionId, "assistant", answer);

        // 7. 智能存储：将有价值的问答存入向量知识库
        if (autoStoreEnabled) {
            storeIfValuable(userMessage, answer);
        }

        return answer;
    }

    // ==================== 知识检索（向量 + 降级） ====================

    private static List<String> retrieveKnowledge(String query) {
        // 优先用向量检索（语义相似度）
        try {
            List<String> results = KnowledgeSearcher.search(query, SEARCH_TOP_K);
            if (!results.isEmpty()) {
                System.out.println("[知识检索] 向量匹配 " + results.size() + " 条");
                return results;
            }
        } catch (Exception e) {
            System.out.println("[知识检索] 向量检索失败，降级为关键词匹配: " + e.getMessage());
        }

        // 降级：MySQL 关键词 LIKE 匹配
        try {
            List<String> fallback = KnowledgeBase.retrieveRelevantChunks(query);
            if (!fallback.isEmpty()) {
                System.out.println("[知识检索] 关键词匹配 " + fallback.size() + " 条");
            }
            return fallback;
        } catch (Exception e) {
            System.out.println("[知识检索] 关键词匹配也失败，无参考知识");
            return new ArrayList<>();
        }
    }

    // ==================== Prompt 构造 ====================

    private static StringBuilder buildPrompt(List<String> knowledge,
                                              List<Map<String, String>> history,
                                              String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个乐于助人的AI助手。请基于以下参考资料和对话历史回答用户的最新问题。\n");
        prompt.append("如果参考资料中有相关信息，请优先使用；如果没有，就用自己的知识回答。\n\n");

        if (!knowledge.isEmpty()) {
            prompt.append("【参考资料】\n");
            for (int i = 0; i < knowledge.size(); i++) {
                prompt.append(i + 1).append(". ").append(knowledge.get(i)).append("\n");
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
        return prompt;
    }

    // ==================== 知识自动存储 ====================

    /**
     * 判断回答是否值得存入知识库，有价值则自动存储
     */
    private static void storeIfValuable(String question, String answer) {
        if (answer == null || answer.length() < MIN_KNOWLEDGE_LENGTH) {
            return; // 太短的回答没存储价值
        }
        // 拼接成 Q&A 格式作为一条知识
        String rawEntry = "问题: " + question + "\n回答: " + answer;
        String knowledgeEntry = cleanKnowledgeText(rawEntry);
        if (knowledgeEntry.length() < 10) {
            System.out.println("[知识存储] 清洗后文本过短，跳过存储");
            return;
        }
        try {
            KnowledgeVectorStore.storeChunk(knowledgeEntry, "chat_qa");
            System.out.println("[知识存储] 已自动存入知识库");
        } catch (Exception e) {
            System.err.println("自动存储失败，问题文本前200字符: " +
                    (question.length() > 200 ? question.substring(0, 200) : question));
            System.err.println("回答文本前200字符: " +
                    (answer.length() > 200 ? answer.substring(0, 200) : answer));
            e.printStackTrace();
        }
    }


    private static String cleanKnowledgeText(String text) {
        if (text == null) return "";
        // 1. 移除 <think>...</think> 标签及其内容（因为那是模型的内部推理，不是有用知识）
        text = text.replaceAll("(?s)<think>.*?</think>", "");
        // 2. 移除其他尖括号标签（如 <br/>、<p> 等）
        text = text.replaceAll("<[^>]+>", " ");
        // 3. 移除控制字符（保留换行、回车、制表符）
        text = text.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", " ");
        // 4. 移除零宽字符、非法代理项
        text = text.replaceAll("\\p{C}", " ");
        // 5. 合并多余空白
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    // ==================== 控制开关 ====================

    public static void setAutoStoreEnabled(boolean enabled) {
        autoStoreEnabled = enabled;
    }

    public static boolean isAutoStoreEnabled() {
        return autoStoreEnabled;
    }
}
