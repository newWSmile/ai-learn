package com.example.ailearn.model.dto.rp;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 工具调用决策结果
 *
 * 说明：
 * 这是模型的第一阶段输出。
 * 模型只负责判断“要不要调用工具、调用哪个工具、参数是什么”。
 */
@Data
public class AiToolCallDecision {

    /**
     * 是否需要调用工具
     */
    private Boolean needTool;

    /**
     * 工具名称
     *
     * 可选值：
     * NONE
     * QUERY_MISSING_KNOWLEDGE
     */
    private String toolName;

    /**
     * 工具参数
     *
     * 例如：
     * {
     *   "limit": 10
     * }
     */
    private Map<String, Object> arguments = new HashMap<>();

    /**
     * 不需要调用工具时的直接回答
     */
    private String directAnswer;
}