package com.example.ailearn.model.dto.rp;

import lombok.Data;

/**
 * 统一AI助手返回结果
 */
@Data
public class AssistantChatResult {

    /**
     * 最终回答
     */
    private String answer;

    /**
     * 路由类型
     *
     * NORMAL_CHAT / RAG_CHAT / TOOL_CALL
     */
    private String routeType;

    /**
     * 是否命中知识库
     *
     * RAG场景下有意义。
     */
    private Boolean knowledgeHit;

    /**
     * 是否使用工具
     *
     * 工具调用场景下有意义。
     */
    private Boolean toolUsed;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 原始结果
     *
     * 学习阶段保留，方便观察不同能力的实际返回。
     * 正式接口可以隐藏。
     */
    private Object rawResult;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;
}
