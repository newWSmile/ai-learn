package com.example.ailearn.model.entity;

import lombok.Data;

/**
 * 知识片段数据库实体
 *
 * 对应表：knowledge_chunk
 */
@Data
public class KnowledgeChunkEntity {

    /**
     * 知识片段ID
     */
    private String id;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 知识分类
     */
    private String category;

    /**
     * 知识来源
     */
    private String source;

    /**
     * 知识正文
     */
    private String content;

    /**
     * 关键词，数据库中先用英文逗号分隔
     */
    private String keywords;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否启用：1启用，0禁用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 修改时间
     */
    private String gmtModified;
}