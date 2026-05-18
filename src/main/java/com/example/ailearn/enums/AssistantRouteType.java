package com.example.ailearn.enums;

/**
 * AI 助手路由类型
 *
 * 说明：
 * 用于判断用户问题应该交给哪个能力处理。
 */
public enum AssistantRouteType {

    /**
     * 普通聊天
     */
    NORMAL_CHAT,

    /**
     * RAG 知识库问答
     */
    RAG_CHAT,

    /**
     * 工具调用
     */
    TOOL_CALL
}
