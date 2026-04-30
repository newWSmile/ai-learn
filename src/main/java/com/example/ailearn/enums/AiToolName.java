package com.example.ailearn.enums;
/**
 * AI 可调用工具名称
 *
 * 说明：
 * Day 26 先只做一个工具，后续 Day 27 再继续扩展。
 */
public enum AiToolName {

    /**
     * 不需要调用工具
     */
    NONE,

    /**
     * 查询最近 RAG 知识库未命中的问题
     */
    QUERY_MISSING_KNOWLEDGE
}