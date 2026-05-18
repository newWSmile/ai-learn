package com.example.ailearn.service;

import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.AssistantRouteType;
import com.example.ailearn.model.dto.rp.AssistantRouteDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * AI助手路由服务
 *
 * 作用：
 * 根据用户问题判断应该走普通聊天、RAG问答还是工具调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantRouterService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 判断路由类型
     */
    public AssistantRouteDecision decide(String question) {
        if (StrUtil.isBlank(question)) {
            AssistantRouteDecision decision = new AssistantRouteDecision();
            decision.setRouteType(AssistantRouteType.NORMAL_CHAT.name());
            decision.setReason("问题为空，走普通聊天兜底");
            return decision;
        }

        // 1. 规则优先：明显查询系统数据的，走工具调用
        AssistantRouteDecision ruleDecision = decideByRules(question);
        if (ruleDecision != null) {
            return ruleDecision;
        }

        // 2. 规则无法判断时，再让模型判断
        return decideByAi(question);
    }

    /**
     * 规则判断
     */
    private AssistantRouteDecision decideByRules(String question) {
        // 工具调用类：查日志、查知识库、查变更记录、查未命中问题
        if (containsAny(question,
                "调用日志", "AI日志", "AI 调用日志", "RAG_CHAT日志", "RAG_CHAT 日志",
                "未命中", "待补知识", "SYSTEM_FALLBACK",
                "知识库列表", "知识库里", "知识片段", "台账类知识", "摄像头相关知识",
                "变更记录", "修改历史", "变更历史", "改过吗")) {
            return buildDecision(AssistantRouteType.TOOL_CALL, "问题涉及系统数据查询，走工具调用");
        }

        // RAG类：业务知识解释、报告表达、怎么分析、怎么写
        if (containsAny(question,
                "摄像头", "点位", "离线", "掉线", "遮挡", "挡住",
                "垃圾桶", "未盖",
                "晨检", "留样", "消毒", "陪餐", "台账",
                "监管报告", "怎么写", "怎么描述", "怎么分析", "属于什么类型", "属于什么问题")) {
            return buildDecision(AssistantRouteType.RAG_CHAT, "问题涉及明厨亮灶业务知识，走RAG问答");
        }

        // 普通问候
        if (containsAny(question, "你好", "您好", "hello", "hi")) {
            return buildDecision(AssistantRouteType.NORMAL_CHAT, "普通问候，走普通聊天");
        }

        return null;
    }

    /**
     * AI判断
     */
    private AssistantRouteDecision decideByAi(String question) {
        String prompt = buildRoutePrompt(question);

        try {
            AssistantRouteDecision decision = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .entity(AssistantRouteDecision.class);

            return normalizeDecision(decision);
        } catch (Exception e) {
            log.error("AI助手路由决策失败，question={}", question, e);
            return buildDecision(AssistantRouteType.NORMAL_CHAT, "路由决策失败，走普通聊天兜底");
        }
    }

    /**
     * 构建路由决策Prompt
     */
    private String buildRoutePrompt(String question) {
        return """
                你是一个AI助手路由决策器。

                你的任务：
                判断用户问题应该交给哪个处理链路。

                可选路由：

                1. NORMAL_CHAT
                普通聊天、问候、闲聊、无需知识库和系统数据查询的问题。

                2. RAG_CHAT
                智慧食堂、明厨亮灶、AI预警、设备运维、台账问题、监管报告表达等业务知识问答。
                示例：
                - 摄像头离线怎么写报告？
                - 垃圾桶未盖属于什么问题？
                - 留样台账没有提交怎么分析？

                3. TOOL_CALL
                查询系统数据、日志、知识库、变更记录、未命中问题等。
                示例：
                - 查一下最近AI调用日志
                - 最近有哪些问题没命中知识库？
                - 查一下知识库里的台账类知识
                - 查一下留样台账未提交说明的变更记录

                输出要求：
                1. 只能输出JSON。
                2. routeType只能是 NORMAL_CHAT、RAG_CHAT、TOOL_CALL。
                3. reason 简要说明原因。
                4. 不要直接回答用户问题，只做路由判断。

                JSON格式：
                {
                  "routeType": "RAG_CHAT",
                  "reason": "问题涉及明厨亮灶业务知识"
                }

                用户问题：
                %s
                """.formatted(question);
    }

    /**
     * 修正AI返回的路由决策
     */
    private AssistantRouteDecision normalizeDecision(AssistantRouteDecision decision) {
        if (decision == null || StrUtil.isBlank(decision.getRouteType())) {
            return buildDecision(AssistantRouteType.NORMAL_CHAT, "AI未返回有效路由，走普通聊天");
        }

        try {
            AssistantRouteType.valueOf(decision.getRouteType());
            return decision;
        } catch (Exception e) {
            log.warn("AI返回非法路由类型，routeType={}", decision.getRouteType());
            return buildDecision(AssistantRouteType.NORMAL_CHAT, "AI返回非法路由，走普通聊天");
        }
    }

    private AssistantRouteDecision buildDecision(AssistantRouteType routeType, String reason) {
        AssistantRouteDecision decision = new AssistantRouteDecision();
        decision.setRouteType(routeType.name());
        decision.setReason(reason);
        return decision;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }

        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}