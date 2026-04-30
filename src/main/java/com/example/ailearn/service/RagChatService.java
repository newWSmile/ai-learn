package com.example.ailearn.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.AiBizType;
import com.example.ailearn.model.dao.AiCallLogRecord;
import com.example.ailearn.model.dto.rp.RagChatResult;
import com.example.ailearn.model.dto.rq.RagChatRequest;
import com.example.ailearn.rag.HybridKnowledgeMatch;
import com.example.ailearn.rag.HybridKnowledgeRetriever;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 问答服务
 * <p>
 * Day 19 改造重点：
 * 1. 使用 HybridKnowledgeRetriever 进行混合检索；
 * 2. 命中知识库时，才调用大模型；
 * 3. 未命中知识库时，不调用大模型，直接系统兜底；
 * 4. RAG_CHAT 继续记录 AI 调用日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    /**
     * ChatClient 构建器
     * <p>
     * 说明：
     * 推荐注入 ChatClient.Builder，而不是直接注入 ChatClient。
     */
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 混合知识检索器
     * <p>
     * Day 19 核心改造点：
     * 原来是 KnowledgeRetriever，现在切换为 HybridKnowledgeRetriever。
     */
    private final HybridKnowledgeRetriever hybridKnowledgeRetriever;

    /**
     * AI 调用日志服务
     */
    private final AiCallLogService aiCallLogService;

    /**
     * JSON 工具
     * <p>
     * 用于把最终结果转换成 JSON 存到 ai_call_log.final_result。
     */
    private final ObjectMapper objectMapper;

    /**
     * 当前聊天模型名称
     */
    @Value("${spring.ai.openai.chat.options.model:qwen3.6-flash}")
    private String modelName;

    /**
     * RAG 问答主入口
     */
    public RagChatResult chat(RagChatRequest request) {
        long startTime = System.currentTimeMillis();

        String question = request == null ? null : request.getQuestion();

        if (StrUtil.isBlank(question)) {
            log.warn("RAG问答失败：question为空");

            RagChatResult result = buildFallbackResult("问题不能为空，请输入需要分析的问题。");

            recordRagChatLog(
                    question,
                    "问题为空，未执行知识库检索，未调用大模型。",
                    result.getAnswer(),
                    result,
                    false,
                    "question为空",
                    System.currentTimeMillis() - startTime,
                    true,
                    "SYSTEM_FALLBACK"
            );

            return result;
        }

        // 1. 使用混合检索器查询知识库
        List<HybridKnowledgeMatch> matches = hybridKnowledgeRetriever.retrieve(question);

        // 2. 如果知识库未命中，不调用大模型，直接系统兜底
        if (CollUtil.isEmpty(matches)) {
            log.info("RAG知识库未命中，不调用大模型，question={}", question);

            RagChatResult result = buildNoHitResult();

            recordRagChatLog(
                    question,
                    "知识库未命中，未调用大模型。",
                    result.getAnswer(),
                    result,
                    true,
                    null,
                    System.currentTimeMillis() - startTime,
                    true,
                    "SYSTEM_FALLBACK"
            );

            return result;
        }

        // 3. 知识库命中后，构建 RAG Prompt
        String prompt = buildRagPrompt(question, matches);

        try {
            log.info("RAG知识库命中，准备调用大模型，question={}，matchCount={}", question, matches.size());

            // 4. 调用大模型生成回答
            String answer = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (StrUtil.isBlank(answer)) {
                answer = "当前 AI 返回内容为空，请稍后重试或补充更明确的问题。";
            }

            // 5. 构建接口返回结果
            RagChatResult result = buildHitResult(answer, matches);

            // 6. 记录 RAG_CHAT 日志
            recordRagChatLog(
                    question,
                    prompt,
                    answer,
                    result,
                    true,
                    null,
                    System.currentTimeMillis() - startTime,
                    result.getNeedReview(),
                    modelName
            );

            return result;
        } catch (Exception e) {
            log.error("RAG调用大模型失败，question={}", question, e);

            RagChatResult result = buildFallbackResult("当前 AI 服务调用异常，暂时无法完成回答。建议稍后重试，或先检查模型服务配置。");

            recordRagChatLog(
                    question,
                    prompt,
                    result.getAnswer(),
                    result,
                    false,
                    e.getMessage(),
                    System.currentTimeMillis() - startTime,
                    true,
                    modelName
            );

            return result;
        }
    }

    /**
     * 构建命中知识库后的 RAG Prompt
     * <p>
     * 说明：
     * 这里的 Prompt 要强调：
     * 1. 只能基于知识库资料回答；
     * 2. 不得编造；
     * 3. 不得把推测原因写成确定事实；
     * 4. 输出不要使用 Markdown，方便前端直接展示。
     */
    private String buildRagPrompt(String question, List<HybridKnowledgeMatch> matches) {
        String knowledgeText = matches.stream()
                .map(match -> """
                        【知识标题】%s
                        【知识分类】%s
                        【知识来源】%s
                        【知识内容】
                        %s
                        """.formatted(
                        match.getTitle(),
                        match.getCategory(),
                        match.getSource(),
                        match.getContent()
                ))
                .collect(Collectors.joining("\n"));

        return """
                你是明厨亮灶监管平台的 AI 分析助手。
                
                请基于下面提供的知识库资料回答用户问题。
                
                【知识库资料】
                %s
                
                【用户问题】
                %s
                
                【回答要求】
                  1. 必须优先基于知识库资料回答。
                  2. 不得编造学校名称、金额、比例、排名。
                  3. 不得使用知识库资料中没有提供的政策、标准、处罚依据。
                  4. 不得把推测原因写成确定事实。
                  5. 回答应采用监管报告口吻，直接给出可使用的报告表述。
                  6. 不要使用“一、二、三”分点标题。
                  7. 不要出现“根据知识库资料”“知识库显示”“资料中提到”等提示语。
                  8. 不得输出 Markdown 格式，不得使用加粗、代码块、表格。
                  9. 数字必须以用户输入和知识库资料为准，不得自行补充。
                  10. 如果涉及处置建议，只能给出通用建议，不得编造具体责任人或学校名称。
                  11. 不要主动复述禁用词清单，只需概括为“避免使用问责性、主观归因类表述”。
                  12. 只有在资料明显不足时，才说明“当前资料仅能支持以下判断”；如果资料已经足够回答用户问题，不要输出这句话。
                  13. 默认回答控制在 150 字以内，除非用户明确要求详细分析。
                """.formatted(knowledgeText, question);
    }

    /**
     * 构建知识命中时的返回结果
     */
    private RagChatResult buildHitResult(String answer, List<HybridKnowledgeMatch> matches) {
        RagChatResult result = new RagChatResult();
        result.setAnswer(answer);
        result.setKnowledgeHit(true);
        result.setNeedReview(false);

        result.setMatchedKnowledgeTitles(
                matches.stream()
                        .map(HybridKnowledgeMatch::getTitle)
                        .distinct()
                        .collect(Collectors.toList())
        );

        result.setMatchedKnowledgeSources(
                matches.stream()
                        .map(HybridKnowledgeMatch::getSource)
                        .distinct()
                        .collect(Collectors.toList())
        );

        return result;
    }

    /**
     * 构建知识未命中时的返回结果
     * <p>
     * 说明：
     * 这里不调用大模型，避免模型凭经验乱补。
     */
    private RagChatResult buildNoHitResult() {
        RagChatResult result = new RagChatResult();
        result.setAnswer("当前知识库资料不足，无法准确回答。建议补充产品文档、接口说明、业务规则或知识库资料后再进行分析。");
        result.setKnowledgeHit(false);
        result.setMatchedKnowledgeTitles(new ArrayList<>());
        result.setMatchedKnowledgeSources(new ArrayList<>());
        result.setNeedReview(true);
        return result;
    }

    /**
     * 构建异常兜底返回结果
     */
    private RagChatResult buildFallbackResult(String answer) {
        RagChatResult result = new RagChatResult();
        result.setAnswer(answer);
        result.setKnowledgeHit(false);
        result.setMatchedKnowledgeTitles(new ArrayList<>());
        result.setMatchedKnowledgeSources(new ArrayList<>());
        result.setNeedReview(true);
        return result;
    }

    /**
     * 记录 RAG_CHAT 日志
     * <p>
     * 说明：
     * 即使知识库未命中，也建议记录日志。
     * 这样后续可以统计哪些问题经常问但知识库没有覆盖。
     */
    private void recordRagChatLog(String question,
                                  String prompt,
                                  String responseText,
                                  RagChatResult result,
                                  boolean success,
                                  String errorMessage,
                                  long costMs,
                                  boolean needReview,
                                  String logModelName) {
        AiCallLogRecord record = new AiCallLogRecord();
        record.setBizType(AiBizType.RAG_CHAT);
        record.setModelName(logModelName);
        record.setUserInput(question);
        record.setPrompt(prompt);
        record.setResponseText(responseText);
        record.setFinalResult(toJsonSafely(result));
        record.setSuccess(success);
        record.setErrorMessage(errorMessage);
        record.setCostMs(costMs);
        record.setNeedReview(needReview);

        aiCallLogService.record(record);
    }

    /**
     * 安全转换 JSON
     * <p>
     * 说明：
     * 转换失败不能影响主流程，所以这里只记录 warn 并返回空 JSON。
     */
    private String toJsonSafely(Object object) {
        if (object == null) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("对象转换JSON失败，objectClass={}", object.getClass().getName(), e);
            return "{}";
        }
    }
}