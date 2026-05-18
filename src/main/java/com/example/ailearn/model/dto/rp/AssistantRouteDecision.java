package com.example.ailearn.model.dto.rp;

import lombok.Data;

/**
 * AI助手路由决策结果
 */
@Data
public class AssistantRouteDecision {

    /**
     * 路由类型
     *
     * NORMAL_CHAT / RAG_CHAT / TOOL_CALL
     */
    private String routeType;

    /**
     * 决策原因
     */
    private String reason;
}
