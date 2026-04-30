package com.example.ailearn.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识片段返回对象
 *
 * 说明：
 * 面向接口返回，不直接暴露数据库里的 keywords 字符串。
 */
@Data
public class KnowledgeChunkVO {

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
     * 关键词列表
     */
    private List<String> keywords = new ArrayList<>();

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 修改时间
     */
    private String gmtModified;
}