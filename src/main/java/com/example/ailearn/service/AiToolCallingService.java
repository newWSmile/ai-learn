package com.example.ailearn.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.AiBizType;
import com.example.ailearn.enums.AiToolName;
import com.example.ailearn.model.dto.rp.AiToolCallDecision;
import com.example.ailearn.model.dto.rp.AiToolChatResult;
import com.example.ailearn.model.dto.rp.KnowledgeChangeLogToolResult;
import com.example.ailearn.model.dto.rq.AiCallLogQueryRequest;
import com.example.ailearn.model.dto.rq.AiToolChatRequest;
import com.example.ailearn.model.vo.AiCallLogListVO;
import com.example.ailearn.model.vo.KnowledgeChangeLogVO;
import com.example.ailearn.model.vo.KnowledgeChunkVO;
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
 * <p>
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
     * AI 调用日志服务
     *
     * 说明：
     * 用于查询最近 AI 调用日志。
     */
    private final AiCallLogQueryService aiCallLogQueryService;

    /**
     * 知识库管理服务
     *
     * 说明：
     * 用于查询知识库列表。
     */
    private final KnowledgeManageService knowledgeManageService;

    /**
     * 知识变更日志服务
     *
     * 说明：
     * 用于查询某条知识的变更记录。
     */
    private final KnowledgeChangeLogService knowledgeChangeLogService;

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

        /**
         * 根据工具名称执行对应后端方法
         */
        switch (AiToolName.valueOf(decision.getToolName())) {
            case QUERY_MISSING_KNOWLEDGE:
                return queryMissingKnowledge(question, decision);

            case QUERY_RECENT_AI_LOGS:
                return queryRecentAiLogs(question, decision);

            case QUERY_KNOWLEDGE_LIST:
                return queryKnowledgeList(question, decision);

            case QUERY_KNOWLEDGE_CHANGE_LOG:
                return queryKnowledgeChangeLog(question, decision);

            case NONE:
            default:
                AiToolChatResult result = new AiToolChatResult();
                result.setAnswer(StrUtil.blankToDefault(decision.getDirectAnswer(), "当前问题不需要调用工具。"));
                result.setToolUsed(false);
                result.setToolName(AiToolName.NONE.name());
                result.setNeedReview(false);
                return result;
        }

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
     * <p>
     * 说明：
     * 这里只让模型做“工具选择和参数抽取”，不让模型编造工具结果。
     */
    private String buildDecisionPrompt(String question) {
        return """
                你是一个 AI 工具调用决策器。
                
                你的任务：
                判断用户问题是否需要调用后端工具，并返回 JSON。
                
                当前可用工具如下：
                
                1. QUERY_MISSING_KNOWLEDGE
                用途：查询最近 RAG 知识库未命中的问题、待补充知识清单、SYSTEM_FALLBACK 问题。
                适用问题示例：
                - 最近有哪些问题没命中知识库？
                - 查一下待补充知识清单
                - 最近有哪些 SYSTEM_FALLBACK？
                - 有哪些问题需要补知识？
                参数：
                - limit：查询数量，默认 10。
                
                2. QUERY_RECENT_AI_LOGS
                用途：查询最近 AI 调用日志。
                适用问题示例：
                - 最近有哪些 AI 调用日志？
                - 查一下最近 RAG_CHAT 日志
                - 最近模型调用是否成功？
                - 查看最近 5 条 AI 日志
                参数：
                - bizType：业务类型，可选，例如 RAG_CHAT、CHAT、RISK_ANALYSIS、WEEKLY_REPORT。
                - limit：查询数量，默认 10。
                
                3. QUERY_KNOWLEDGE_LIST
                用途：查询知识库列表。
                适用问题示例：
                - 查一下知识库有哪些内容
                - 查询台账类知识
                - 有哪些启用中的知识片段？
                - 搜索摄像头相关知识
                参数：
                - category：知识分类，可选，例如 LEDGER_RULE、DEVICE_OPERATION、REPORT_STYLE、ALARM_EXPLANATION。
                - enabled：是否启用，可选，true 或 false。
                - keyword：关键词，可选。
                - limit：查询数量，默认 20。
                
                4. QUERY_KNOWLEDGE_CHANGE_LOG
               用途：查询某条知识片段的变更日志。
               适用问题示例：
               - 查一下 mclz_sample_record_not_submitted 的变更记录
               - 留样台账未提交说明最近改过吗？
               - 查询某条知识的修改历史
               - 查一下摄像头离线说明的变更记录
               参数：
               - knowledgeId：知识片段ID，可选。如果用户明确提供 mclz_ 或 kc_ 开头的ID，应填写。
               - keyword：知识标题或关键词，可选。如果用户没有提供 knowledgeId，但提供了知识标题，例如“留样台账未提交说明”，应填写 keyword。
               - limit：查询数量，默认 10。
                
                5. NONE
                用途：不需要调用工具。
                
                输出要求：
                1. 只能输出 JSON。
                2. 不要输出 Markdown。
                3. toolName 只能是：
                   NONE、
                   QUERY_MISSING_KNOWLEDGE、
                   QUERY_RECENT_AI_LOGS、
                   QUERY_KNOWLEDGE_LIST、
                   QUERY_KNOWLEDGE_CHANGE_LOG。
                4. 如果需要调用工具，needTool=true。
                5. 如果不需要调用工具，needTool=false，toolName=NONE，并在 directAnswer 中给出简短回答。
                6. 不得编造工具结果，工具结果由 Java 后端执行。
                7. 如果问题里出现“最近 N 条”，arguments.limit 应填写 N。
                8. 如果用户明确给出 knowledgeId，例如 mclz_sample_record_not_submitted，应放入 arguments.knowledgeId。
                9. 如果用户没有提供 knowledgeId，但提供了知识标题或关键词，应放入 arguments.keyword。
                
                JSON 示例：
                {
                  "needTool": true,
                  "toolName": "QUERY_KNOWLEDGE_LIST",
                  "arguments": {
                    "category": "LEDGER_RULE",
                    "enabled": true,
                    "limit": 20
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

        boolean validToolName = false;
        for (AiToolName item : AiToolName.values()) {
            if (item.name().equals(decision.getToolName())) {
                validToolName = true;
                break;
            }
        }

        if (!validToolName) {
            log.warn("AI返回非法工具名称，toolName={}，已修正为NONE", decision.getToolName());
            decision.setToolName(AiToolName.NONE.name());
            decision.setNeedTool(false);
        }

        if (decision.getNeedTool() == null) {
            decision.setNeedTool(!AiToolName.NONE.name().equals(decision.getToolName()));
        }

        if (decision.getArguments() == null) {
            decision.setArguments(new java.util.HashMap<>());
        }

        return decision;
    }

    /**
     * 执行工具：查询 RAG 知识库未命中问题
     */
    private AiToolChatResult queryMissingKnowledge(String question, AiToolCallDecision decision) {
        int limit = getIntArgument(decision.getArguments(), "limit", -1);

        // 模型没抽取到数量时，Java 兜底从问题中解析“最近N条”
        if (limit <= 0) {
            limit = parseLimitFromQuestion(question, 10);
        }

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

    /**
     * 将模型返回的 bizType 字符串转换为 AiBizType 枚举
     *
     * 说明：
     * 模型 arguments 里传回来的通常是字符串，例如 RAG_CHAT。
     */
    private AiBizType parseBizTypeFromText(String bizTypeText) {
        if (StrUtil.isBlank(bizTypeText)) {
            return null;
        }

        try {
            return AiBizType.valueOf(bizTypeText.trim());
        } catch (Exception e) {
            log.warn("AI返回的bizType不合法，bizTypeText={}", bizTypeText);
            return null;
        }
    }

    /**
     * 从用户问题中兜底解析业务类型
     */
    private AiBizType parseBizTypeFromQuestion(String question) {
        if (StrUtil.isBlank(question)) {
            return null;
        }

        if (question.contains("RAG_CHAT")
                || question.contains("RAG问答")
                || question.contains("RAG 日志")
                || question.contains("RAG日志")) {
            return AiBizType.RAG_CHAT;
        }

        if (question.contains("WEEKLY_REPORT")
                || question.contains("周报")) {
            return AiBizType.WEEKLY_REPORT;
        }

        if (question.contains("RISK_ANALYSIS")
                || question.contains("风险分析")) {
            return AiBizType.RISK_ANALYSIS;
        }

        if (question.contains("CHAT")
                || question.contains("普通对话")) {
            return AiBizType.CHAT;
        }

        return null;
    }

    /**
     * 执行工具：查询最近 AI 调用日志
     */
    private AiToolChatResult queryRecentAiLogs(String question, AiToolCallDecision decision) {

        String bizTypeText = getStringArgument(decision.getArguments(), "bizType", null);

        // 如果模型没有抽取到 bizType，则从用户问题中兜底解析
        AiBizType bizType = parseBizTypeFromText(bizTypeText);
        if (bizType == null) {
            bizType = parseBizTypeFromQuestion(question);
        }

        int limit = getIntArgument(decision.getArguments(), "limit", -1);

        if (limit <= 0) {
            limit = parseLimitFromQuestion(question, 10);
        }

        // 这里按你项目现有方法替换即可
        AiCallLogQueryRequest queryRequest = new AiCallLogQueryRequest();
        queryRequest.setBizType(bizType);
        queryRequest.setLimit(limit);

        List<AiCallLogListVO> list = aiCallLogQueryService.recent(queryRequest);



        AiToolChatResult result = new AiToolChatResult();
        result.setToolUsed(true);
        result.setToolName(AiToolName.QUERY_RECENT_AI_LOGS.name());
        result.setToolResult(list);
        result.setNeedReview(false);
        result.setAnswer(buildAiLogAnswer(list));

        log.info("工具执行完成，toolName={}，bizType={}，limit={}，resultCount={}",
                AiToolName.QUERY_RECENT_AI_LOGS.name(), bizType, limit, list == null ? 0 : list.size());

        return result;
    }

    /**
     * 将 AI 日志工具结果组织成回答
     */
    private String buildAiLogAnswer(List<AiCallLogListVO> list) {
        if (CollUtil.isEmpty(list)) {
            return "当前没有查询到最近的 AI 调用日志。";
        }

        String items = list.stream()
                .map(item -> "业务类型：" + item.getBizType()
                        + "；模型：" + item.getModelName()
                        + "；问题：" + item.getUserInput()
                        + "；成功：" + item.getSuccess()
                        + "；需复核：" + item.getNeedReview()
                        + "；耗时：" + item.getCostMs() + "ms"
                        + "；时间：" + item.getGmtCreate())
                .collect(Collectors.joining("\n"));

        return "最近的 AI 调用日志如下：\n" + items;
    }


    /**
     * 执行工具：查询知识库列表
     */
    private AiToolChatResult queryKnowledgeList(String question, AiToolCallDecision decision) {
        String category = getStringArgument(decision.getArguments(), "category", null);
        String keyword = getStringArgument(decision.getArguments(), "keyword", null);
        Boolean enabled = getBooleanArgument(decision.getArguments(), "enabled", true);

        int limit = getIntArgument(decision.getArguments(), "limit", -1);
        if (limit <= 0) {
            limit = parseLimitFromQuestion(question, 20);
        }

        List<KnowledgeChunkVO> list = knowledgeManageService.list(category, enabled, keyword, limit);

        AiToolChatResult result = new AiToolChatResult();
        result.setToolUsed(true);
        result.setToolName(AiToolName.QUERY_KNOWLEDGE_LIST.name());
        result.setToolResult(list);
        result.setNeedReview(false);
        result.setAnswer(buildKnowledgeListAnswer(list));

        log.info("工具执行完成，toolName={}，category={}，enabled={}，keyword={}，limit={}，resultCount={}",
                AiToolName.QUERY_KNOWLEDGE_LIST.name(), category, enabled, keyword, limit, list == null ? 0 : list.size());

        return result;
    }


    /**
     * 将知识库列表工具结果组织成回答
     */
    private String buildKnowledgeListAnswer(List<KnowledgeChunkVO> list) {
        if (CollUtil.isEmpty(list)) {
            return "当前没有查询到符合条件的知识片段。";
        }

        String items = list.stream()
                .map(item -> "标题：" + item.getTitle()
                        + "；分类：" + item.getCategory()
                        + "；来源：" + item.getSource()
                        + "；启用：" + item.getEnabled()
                        + "；优先级：" + item.getPriority())
                .collect(Collectors.joining("\n"));

        return "查询到的知识片段如下：\n" + items;
    }



    /**
     * 执行工具：查询知识变更日志
     *
     * 支持两种方式：
     * 1. 用户直接提供 knowledgeId；
     * 2. 用户提供知识标题 / 关键词，系统先反查知识片段，再查询变更记录。
     */
    private AiToolChatResult queryKnowledgeChangeLog(String question, AiToolCallDecision decision) {
        String knowledgeId = getStringArgument(decision.getArguments(), "knowledgeId", null);
        String keyword = getStringArgument(decision.getArguments(), "keyword", null);

        int limit = getIntArgument(decision.getArguments(), "limit", -1);
        if (limit <= 0) {
            limit = parseLimitFromQuestion(question, 10);
        }

        // 1. 如果模型没有抽取 knowledgeId，则从用户原始问题中兜底解析
        if (StrUtil.isBlank(knowledgeId)) {
            knowledgeId = parseKnowledgeIdFromQuestion(question);
        }

        // 2. 如果没有 knowledgeId，则尝试从问题中抽取标题关键词
        if (StrUtil.isBlank(keyword)) {
            keyword = parseKnowledgeKeywordFromQuestion(question);
        }

        KnowledgeChangeLogToolResult toolResult = new KnowledgeChangeLogToolResult();

        // 3. 如果有 knowledgeId，直接查询变更日志
        if (StrUtil.isNotBlank(knowledgeId)) {
            return buildKnowledgeChangeLogByIdResult(knowledgeId, limit, toolResult);
        }

        // 4. 没有 knowledgeId，也没有 keyword，无法查询
        if (StrUtil.isBlank(keyword)) {
            AiToolChatResult result = new AiToolChatResult();
            result.setToolUsed(true);
            result.setToolName(AiToolName.QUERY_KNOWLEDGE_CHANGE_LOG.name());
            result.setNeedReview(true);
            result.setToolResult(toolResult);
            result.setAnswer("请提供要查询的知识片段ID或知识标题，例如 mclz_sample_record_not_submitted 或 留样台账未提交说明。");
            return result;
        }

        // 5. 根据标题 / 关键词查询候选知识
        List<KnowledgeChunkVO> candidates = knowledgeManageService.searchCandidates(keyword, 10);
        toolResult.setCandidates(candidates);

        if (CollUtil.isEmpty(candidates)) {
            AiToolChatResult result = new AiToolChatResult();
            result.setToolUsed(true);
            result.setToolName(AiToolName.QUERY_KNOWLEDGE_CHANGE_LOG.name());
            result.setNeedReview(true);
            result.setToolResult(toolResult);
            result.setAnswer("没有找到与“" + keyword + "”匹配的知识片段，请确认知识标题或提供知识ID。");
            return result;
        }

        // 5.1 精确标题优先
        KnowledgeChunkVO exactKnowledge = resolveExactKnowledge(keyword, candidates);
        if (exactKnowledge != null) {
            return buildKnowledgeChangeLogByIdResult(exactKnowledge.getId(), limit, toolResult);
        }

        // 6. 如果候选只有一条，自动使用该知识ID查询变更日志
        if (candidates.size() == 1) {
            KnowledgeChunkVO candidate = candidates.get(0);
            return buildKnowledgeChangeLogByIdResult(candidate.getId(), limit, toolResult);
        }

        // 7. 如果候选多条，不自动猜，提示用户选择
        AiToolChatResult result = new AiToolChatResult();
        result.setToolUsed(true);
        result.setToolName(AiToolName.QUERY_KNOWLEDGE_CHANGE_LOG.name());
        result.setNeedReview(true);
        result.setToolResult(toolResult);
        result.setAnswer(buildCandidateSelectionAnswer(keyword, candidates));
        return result;
    }

    /**
     * 根据知识ID查询变更日志并构建工具返回
     */
    private AiToolChatResult buildKnowledgeChangeLogByIdResult(String knowledgeId,
                                                               int limit,
                                                               KnowledgeChangeLogToolResult toolResult) {
        KnowledgeChunkVO knowledge = null;

        try {
            knowledge = knowledgeManageService.detail(knowledgeId);
        } catch (Exception e) {
            log.warn("查询知识详情失败，knowledgeId={}", knowledgeId, e);
        }

        List<KnowledgeChangeLogVO> list = knowledgeChangeLogService.listByKnowledgeId(knowledgeId, limit);

        toolResult.setResolved(true);
        toolResult.setResolvedKnowledgeId(knowledgeId);
        toolResult.setResolvedTitle(knowledge == null ? null : knowledge.getTitle());
        toolResult.setChangeLogs(list);

        AiToolChatResult result = new AiToolChatResult();
        result.setToolUsed(true);
        result.setToolName(AiToolName.QUERY_KNOWLEDGE_CHANGE_LOG.name());
        result.setToolResult(toolResult);
        result.setNeedReview(false);
        result.setAnswer(buildKnowledgeChangeLogAnswer(
                knowledgeId,
                knowledge == null ? null : knowledge.getTitle(),
                list
        ));

        log.info("工具执行完成，toolName={}，knowledgeId={}，limit={}，resultCount={}",
                AiToolName.QUERY_KNOWLEDGE_CHANGE_LOG.name(),
                knowledgeId,
                limit,
                list == null ? 0 : list.size());

        return result;
    }


    /**
     * 将知识变更日志工具结果组织成回答
     */
    /**
     * 将知识变更日志工具结果组织成回答
     */
    private String buildKnowledgeChangeLogAnswer(String knowledgeId,
                                                 String title,
                                                 List<KnowledgeChangeLogVO> list) {
        String displayName = StrUtil.isBlank(title)
                ? knowledgeId
                : title + "（" + knowledgeId + "）";

        if (CollUtil.isEmpty(list)) {
            return "知识片段 " + displayName + " 暂无变更记录。";
        }

        String items = list.stream()
                .map(item -> "操作：" + item.getOperationType()
                        + "；说明：" + item.getRemark()
                        + "；操作人：" + item.getOperator()
                        + "；时间：" + item.getGmtCreate())
                .collect(Collectors.joining("\n"));

        return "知识片段 " + displayName + " 的变更记录如下：\n" + items;
    }

    /**
     * 构建候选知识选择提示
     */
    private String buildCandidateSelectionAnswer(String keyword, List<KnowledgeChunkVO> candidates) {
        String items = candidates.stream()
                .map(item -> "ID：" + item.getId()
                        + "；标题：" + item.getTitle()
                        + "；分类：" + item.getCategory()
                        + "；启用：" + item.getEnabled())
                .collect(Collectors.joining("\n"));

        return "根据“" + keyword + "”找到多条可能的知识片段，请指定要查询的知识ID：\n" + items;
    }

    /**
     * 从用户问题中兜底解析知识标题 / 关键词
     *
     * 示例：
     * 查一下留样台账未提交说明的变更记录 -> 留样台账未提交说明
     * 摄像头离线说明最近改过吗 -> 摄像头离线说明
     */
    private String parseKnowledgeKeywordFromQuestion(String question) {
        if (StrUtil.isBlank(question)) {
            return null;
        }

        String text = question;

        // 去掉常见动作词
        text = text.replace("查一下", "")
                .replace("查询", "")
                .replace("看一下", "")
                .replace("查看", "")
                .replace("最近", "")
                .replace("有没有", "")
                .replace("是否", "");

        // 去掉变更记录类尾巴
        text = text.replace("的变更记录", "")
                .replace("变更记录", "")
                .replace("修改历史", "")
                .replace("变更历史", "")
                .replace("最近改过吗", "")
                .replace("改过吗", "")
                .replace("更新记录", "")
                .replace("操作记录", "");

        text = text.trim();

        // 如果清理后太短，认为无法解析
        if (text.length() < 2) {
            return null;
        }

        return text;
    }

    /**
     * 根据关键词解析唯一知识片段
     *
     * 规则：
     * 1. 先从候选里找 title 完全等于 keyword 的；
     * 2. 如果精确匹配只有一条，直接返回；
     * 3. 否则返回 null，交给多候选逻辑处理。
     */
    private KnowledgeChunkVO resolveExactKnowledge(String keyword, List<KnowledgeChunkVO> candidates) {
        if (StrUtil.isBlank(keyword) || CollUtil.isEmpty(candidates)) {
            return null;
        }

        List<KnowledgeChunkVO> exactList = candidates.stream()
                .filter(item -> keyword.equals(item.getTitle()))
                .collect(Collectors.toList());

        if (exactList.size() == 1) {
            return exactList.get(0);
        }

        return null;
    }

    /**
     * 从 arguments 中读取字符串参数
     */
    private String getStringArgument(Map<String, Object> arguments, String key, String defaultValue) {
        if (arguments == null || !arguments.containsKey(key)) {
            return defaultValue;
        }

        Object value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }

        String text = value.toString();
        return StrUtil.isBlank(text) ? defaultValue : text;
    }

    /**
     * 从 arguments 中读取布尔参数
     */
    private Boolean getBooleanArgument(Map<String, Object> arguments, String key, Boolean defaultValue) {
        if (arguments == null || !arguments.containsKey(key)) {
            return defaultValue;
        }

        Object value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        String text = value.toString();
        if ("true".equalsIgnoreCase(text) || "1".equals(text) || "是".equals(text) || "启用".equals(text)) {
            return true;
        }

        if ("false".equalsIgnoreCase(text) || "0".equals(text) || "否".equals(text) || "禁用".equals(text)) {
            return false;
        }

        return defaultValue;
    }

    /**
     * 从用户问题中兜底解析 limit
     *
     * 示例：
     * 最近 5 条 RAG 未命中问题 -> 5
     */
    private int parseLimitFromQuestion(String question, int defaultValue) {
        if (StrUtil.isBlank(question)) {
            return defaultValue;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("最近\\s*(\\d+)\\s*条");
        java.util.regex.Matcher matcher = pattern.matcher(question);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception e) {
                log.warn("从问题中解析limit失败，question={}", question, e);
            }
        }

        return defaultValue;
    }

    /**
     * 从用户问题中兜底解析知识片段ID
     *
     * 说明：
     * 当前知识ID一般以 mclz_ 或 kc_ 开头。
     */
    private String parseKnowledgeIdFromQuestion(String question) {
        if (StrUtil.isBlank(question)) {
            return null;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(mclz_[a-zA-Z0-9_]+|kc_[a-zA-Z0-9_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(question);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}