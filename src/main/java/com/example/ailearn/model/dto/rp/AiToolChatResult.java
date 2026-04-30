package com.example.ailearn.model.dto.rp;

import lombok.Data;

/**
 * AI 工具调用接口返回结果
 */
@Data
public class AiToolChatResult {

    /**
     * 最终回答
     */
    private String answer;

    /**
     * 是否调用了工具
     */
    private Boolean toolUsed;

    /**
     * 实际调用的工具名称
     */
    private String toolName;

    /**
     * 工具原始返回结果
     *
     * 学习阶段可以直接返回，方便你观察工具调用效果。
     * 后续正式接口可以隐藏这个字段。
     */
    private Object toolResult;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;
}