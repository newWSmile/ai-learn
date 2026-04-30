package com.example.ailearn.model.vo;

import lombok.Data;

/**
 * 知识片段变更日志返回对象
 */
@Data
public class KnowledgeChangeLogVO {

    /**
     * 日志ID
     */
    private String id;

    /**
     * 知识片段ID
     */
    private String knowledgeId;

    /**
     * 操作类型
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
     * 操作人
     */
    private String operator;

    /**
     * 创建时间
     */
    private String gmtCreate;
}