package com.example.ailearn.model.entity;

import lombok.Data;

/**
 * 知识片段变更日志实体
 *
 * 对应表：knowledge_chunk_change_log
 */
@Data
public class KnowledgeChunkChangeLogEntity {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 知识片段ID
     */
    private String knowledgeId;

    /**
     * 操作类型：CREATE / UPDATE / ENABLE / DISABLE
     */
    private String operationType;

    /**
     * 变更前数据，JSON字符串
     */
    private String beforeData;

    /**
     * 变更后数据，JSON字符串
     */
    private String afterData;

    /**
     * 变更说明
     */
    private String remark;

    /**
     * 操作人，学习阶段默认 SYSTEM
     */
    private String operator;

    /**
     * 创建时间
     */
    private String gmtCreate;
}
