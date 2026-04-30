package com.example.ailearn.service;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.AiToolName;
import com.example.ailearn.model.dto.rp.AiToolCallDecision;
import com.example.ailearn.model.dto.rp.AiToolChatResult;
import com.example.ailearn.model.dto.rq.AiToolChatRequest;
import com.example.ailearn.model.vo.MissingKnowledgeItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 工具调用服务
 *
 * Day 26 目标：
 * 1. 让 AI 判断是否需要调用工具；
 * 2. Java 根据 AI 的结构化决策执行工具；
 * 3. 工具结果由 Java 组织成最终回答。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolCallingService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 复用 Day 20 的待补知识服务
     */
    private final MissingKnowledgeService missingKnowledgeService;

    /**
     * AI 工具调用入口
     */
    public AiToolChatResult chat(AiToolChatRequest request) {
        String question = request == null ? null : request.getQuestion();

        if (StrUtil.isBlank(question)) {
            AiToolChatResult result = new AiToolChatResult();
            result.setAnswer("问题不能为空，请输入需要处理的问题。");
            result.setToolUsed(false);
            result.setToolName(AiToolName.NONE.name());
            result.setNeedReview(true);
            return result;
        }

        // 1. 先让 AI 判断是否需要调用工具
        AiToolCallDecision decision = decideTool(question);

        // 2. 对模型返回的工具决策做兜底修正，避免模型返回非法工具名
        decision = normalizeDecision(decision);

        log.info("AI工具调用决策完成，question={}，needTool={}，toolName={}，arguments={}",
                question, decision.getNeedTool(), decision.getToolName(), decision.getArguments());

        // 3. 不需要调用工具时，直接返回模型给出的直接回答
        if (!Boolean.TRUE.equals(decision.getNeedTool())
                || AiToolName.NONE.name().equals(decision.getToolName())) {
            AiToolChatResult result = new AiToolChatResult();
            result.setAnswer(StrUtil.blankToDefault(decision.getDirectAnswer(), "当前问题不需要调用工具。"));
            result.setToolUsed(false);
            result.setToolName(AiToolName.NONE.name());
            result.setNeedReview(false);
            return result;
        }

        // 4. 根据工具名称执行对应后端方法
        if (AiToolName.QUERY_MISSING_KNOWLEDGE.name().equals(decision.getToolName())) {
            return queryMissingKnowledge(decision);
        }

        // 5. 理论上不会走到这里，兜底处理
        AiToolChatResult result = new AiToolChatResult();
        result.setAnswer("当前工具暂不支持，请检查工具名称配置。");
        result.setToolUsed(false);
        result.setToolName(decision.getToolName());
        result.setNeedReview(true);
        return result;
    }

    /**
     * 第一阶段：让模型判断是否需要调用工具
     */
    private AiToolCallDecision decideTool(String question) {
        String prompt = buildDecisionPrompt(question);

        try {
            return chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .entity(AiToolCallDecision.class);
        } catch (Exception e) {
            log.error("AI工具调用决策失败，question={}", question, e);

            AiToolCallDecision fallback = new AiToolCallDecision();
            fallback.setNeedTool(false);
            fallback.setToolName(AiToolName.NONE.name());
            fallback.setDirectAnswer("当前 AI 工具调用决策失败，请稍后重试。");
            return fallback;
        }
    }

    /**
     * 构建工具调用决策 Prompt
     *
     * 说明：
     * 这里只让模型做“决策”，不让模型执行工具。
     */
    private String buildDecisionPrompt(String question) {
        return """
                你是一个 AI 工具调用决策器。

                你的任务：
                判断用户问题是否需要调用后端工具，并返回 JSON。

                当前可用工具只有：

                1. QUERY_MISSING_KNOWLEDGE
                用途：查询最近 RAG 知识库未命中的问题、待补充知识清单、SYSTEM_FALLBACK 问题。
                适用问题示例：
                - 最近有哪些问题没命中知识库？
                - 查一下待补充知识清单
                - 最近有哪些 SYSTEM_FALLBACK？
                - 有哪些问题需要补知识？
                - 查一下 RAG 未命中记录

                2. NONE
                用途：不需要调用工具。

                输出要求：
                1. 只能输出 JSON。
                2. 不要输出 Markdown。
                3. toolName 只能是 NONE 或 QUERY_MISSING_KNOWLEDGE。
                4. 如果调用 QUERY_MISSING_KNOWLEDGE，arguments 中可以包含 limit，默认 10。
                5. 如果不需要调用工具，needTool=false，toolName=NONE，并在 directAnswer 中给出简短回答。
                6. 不得编造工具结果，工具结果由 Java 后端执行。

                JSON 格式：
                {
                  "needTool": true,
                  "toolName": "QUERY_MISSING_KNOWLEDGE",
                  "arguments": {
                    "limit": 10
                  },
                  "directAnswer": null
                }

                用户问题：
                %s
                """.formatted(question);
    }

    /**
     * 修正模型返回的工具决策
     *
     * 说明：
     * 模型可能返回空对象、非法 toolName 或缺少字段，这里统一兜底。
     */
    private AiToolCallDecision normalizeDecision(AiToolCallDecision decision) {
        if (decision == null) {
            AiToolCallDecision fallback = new AiToolCallDecision();
            fallback.setNeedTool(false);
            fallback.setToolName(AiToolName.NONE.name());
            fallback.setDirectAnswer("当前无法判断是否需要调用工具。");
            return fallback;
        }

        if (StrUtil.isBlank(decision.getToolName())) {
            decision.setToolName(AiToolName.NONE.name());
        }

        boolean validToolName = AiToolName.NONE.name().equals(decision.getToolName())
                || AiToolName.QUERY_MISSING_KNOWLEDGE.name().equals(decision.getToolName());

        if (!validToolName) {
            log.warn("AI返回非法工具名称，toolName={}，已修正为NONE", decision.getToolName());
            decision.setToolName(AiToolName.NONE.name());
            decision.setNeedTool(false);
        }

        if (decision.getNeedTool() == null) {
            decision.setNeedTool(!AiToolName.NONE.name().equals(decision.getToolName()));
        }

        return decision;
    }

    /**
     * 执行工具：查询 RAG 知识库未命中问题
     */
    private AiToolChatResult queryMissingKnowledge(AiToolCallDecision decision) {
        int limit = getIntArgument(decision.getArguments(), "limit", 10);

        List<MissingKnowledgeItemVO> list = missingKnowledgeService.listRecent(limit);

        AiToolChatResult result = new AiToolChatResult();
        result.setToolUsed(true);
        result.setToolName(AiToolName.QUERY_MISSING_KNOWLEDGE.name());
        result.setToolResult(list);
        result.setNeedReview(false);
        result.setAnswer(buildMissingKnowledgeAnswer(list));

        log.info("工具执行完成，toolName={}，limit={}，resultCount={}",
                AiToolName.QUERY_MISSING_KNOWLEDGE.name(), limit, list == null ? 0 : list.size());

        return result;
    }

    /**
     * 将待补知识工具结果组织成回答
     */
    private String buildMissingKnowledgeAnswer(List<MissingKnowledgeItemVO> list) {
        if (CollUtil.isEmpty(list)) {
            return "当前没有查询到最近的 RAG 知识库未命中问题。";
        }

        String items = list.stream()
                .map(item -> "问题：" + item.getQuestion()
                        + "；建议分类：" + item.getSuggestedCategory()
                        + "；建议处理：" + item.getSuggestedAction())
                .collect(Collectors.joining("\n"));

        return "最近的 RAG 知识库未命中问题如下：\n" + items;
    }

    /**
     * 从 arguments 中读取整数参数
     */
    private int getIntArgument(Map<String, Object> arguments, String key, int defaultValue) {
        if (arguments == null || !arguments.containsKey(key)) {
            return defaultValue;
        }

        Object value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof Number number) {
                return number.intValue();
            }

            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.warn("读取工具参数失败，key={}，value={}，使用默认值={}", key, value, defaultValue);
            return defaultValue;
        }
    }
}