package com.example.ailearn.model.dto.rq;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 新增知识片段请求
 */
@Data
public class KnowledgeCreateRequest {

    /**
     * 知识片段ID
     *
     * 说明：
     * 可以前端传入业务可读ID，例如 mclz_morning_check_not_submitted。
     * 如果为空，后端自动生成。
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
}