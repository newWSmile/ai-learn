package com.example.ailearn.enums;



/**
 * AI 可调用工具名称
 * 说明：
 * 工具名称必须稳定，后续 Prompt、Service 分发逻辑都依赖这些枚举值。
 */
public enum AiToolName {

    /**
     * 不需要调用工具
     */
    NONE,

    /**
     * 查询最近 RAG 知识库未命中的问题
     */
    QUERY_MISSING_KNOWLEDGE,

    /**
     * 查询最近 AI 调用日志
     */
    QUERY_RECENT_AI_LOGS,

    /**
     * 查询知识库列表
     */
    QUERY_KNOWLEDGE_LIST,

    /**
     * 查询某条知识的变更日志
     */
    QUERY_KNOWLEDGE_CHANGE_LOG
}