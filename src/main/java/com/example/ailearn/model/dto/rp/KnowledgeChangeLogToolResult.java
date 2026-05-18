package com.example.ailearn.model.dto.rp;

import com.example.ailearn.model.vo.KnowledgeChangeLogVO;
import com.example.ailearn.model.vo.KnowledgeChunkVO;
import lombok.Data;

import java.util.List;

/**
 * 知识变更日志工具返回结果
 *
 * 说明：
 * 用于 Function Calling 工具结果。
 * 既能返回最终变更日志，也能返回候选知识列表。
 */
@Data
public class KnowledgeChangeLogToolResult {

    /**
     * 是否成功解析到唯一知识片段
     */
    private Boolean resolved;

    /**
     * 最终解析到的知识片段ID
     */
    private String resolvedKnowledgeId;

    /**
     * 最终解析到的知识标题
     */
    private String resolvedTitle;

    /**
     * 候选知识片段
     *
     * 如果命中多条，需要提示用户选择。
     */
    private List<KnowledgeChunkVO> candidates;

    /**
     * 变更日志列表
     */
    private List<KnowledgeChangeLogVO> changeLogs;
}