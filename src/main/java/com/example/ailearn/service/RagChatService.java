package com.example.ailearn.service;


import com.example.ailearn.enums.AiBizType;
import com.example.ailearn.model.dao.AiCallLogRecord;
import com.example.ailearn.model.dto.rp.RagChatResult;
import com.example.ailearn.model.dto.rq.RagChatRequest;
import com.example.ailearn.rag.KnowledgeChunk;
import com.example.ailearn.rag.KnowledgeRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private final ChatClient.Builder chatClientBuilder;

    private final KnowledgeRetriever knowledgeRetriever;

    private final AiCallLogService aiCallLogService;

    @Value("${spring.ai.openai.chat.options.model:qwen3.6-flash}")
    private String modelName;

    public RagChatResult chat(RagChatRequest request) {
        long start = System.currentTimeMillis();

        String question = request == null ? null : request.getQuestion();

        List<KnowledgeChunk> chunks = knowledgeRetriever.retrieve(question);
        boolean knowledgeHit = !chunks.isEmpty();

        String prompt = buildPrompt(question, chunks);

        try {
            String answer = chatClientBuilder.build().prompt()
                    .user(prompt)
                    .call()
                    .content();

            RagChatResult result = RagChatResult.builder()
                    .answer(answer)
                    .knowledgeHit(knowledgeHit)
                    .matchedKnowledgeTitles(chunks.stream().map(KnowledgeChunk::getTitle).toList())
                    .needReview(!knowledgeHit)
                    .build();

            aiCallLogService.record(AiCallLogRecord.builder()
                    .bizType(AiBizType.RAG_CHAT)
                    .modelName(modelName)
                    .userInput(question)
                    .prompt(prompt)
                    .responseText(answer)
                    .finalResult(toJsonSafely(result))
                    .success(true)
                    .errorMessage(null)
                    .costMs(System.currentTimeMillis() - start)
                    .needReview(result.getNeedReview())
                    .build());

            return result;
        } catch (Exception e) {
            log.error("RAG问答调用失败, question={}", question, e);

            RagChatResult fallback = RagChatResult.builder()
                    .answer("当前AI问答服务暂不可用，请稍后重试。")
                    .knowledgeHit(knowledgeHit)
                    .matchedKnowledgeTitles(chunks.stream().map(KnowledgeChunk::getTitle).toList())
                    .needReview(true)
                    .build();

            aiCallLogService.record(AiCallLogRecord.builder()
                    .bizType(AiBizType.RAG_CHAT)
                    .modelName(modelName)
                    .userInput(question)
                    .prompt(prompt)
                    .responseText(null)
                    .finalResult(toJsonSafely(fallback))
                    .success(false)
                    .errorMessage(e.getMessage())
                    .costMs(System.currentTimeMillis() - start)
                    .needReview(true)
                    .build());

            return fallback;
        }
    }

    private String buildPrompt(String question, List<KnowledgeChunk> chunks) {
        String knowledgeText = buildKnowledgeText(chunks);

        return """
                你是智慧食堂和明厨亮灶监管平台的AI助手。
                
                请根据【知识库资料】回答用户问题。
                
                回答规则：
                 1. 优先基于知识库资料回答；
                 2. 如果知识库资料不足以回答问题，必须说明“当前知识库资料不足，无法准确回答”；
                 3. 不得编造学校名称、金额、比例、排名；
                 4. 不得使用知识库资料中没有提供的政策、标准、处罚依据；
                 5. 回答应正式、简洁，适合产品经理、后端工程师或教育局管理人员阅读；
                 6. 不得输出 Markdown 格式，不得使用加粗、代码块、表格；
                 7. 不得把推测原因写成确定事实；
                 8. 如果知识库资料已命中，回答时不必重复说明“根据知识库资料”，直接给出结论；
                 9. 如果知识库资料未命中，不得尝试凭经验补充回答；
                 10. 如果知识库资料未命中，可以建议用户补充产品文档、接口说明、业务规则或知识库资料。
                
                【知识库资料】
                %s
                
                【用户问题】
                %s
                """.formatted(knowledgeText, question);
    }

    private String buildKnowledgeText(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "未检索到相关知识片段。";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            builder.append("资料").append(i + 1).append("：").append("\n");
            builder.append("标题：").append(chunk.getTitle()).append("\n");
            builder.append("内容：").append(chunk.getContent()).append("\n");
            builder.append("\n");
        }

        return builder.toString();
    }

    private String toJsonSafely(Object object) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            log.warn("对象转JSON失败", e);
            return String.valueOf(object);
        }
    }
}