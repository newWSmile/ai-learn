package com.example.ailearn.rag;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunk {

    /**
     * 知识片段ID，全局唯一
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
     * 知识来源，例如：系统内置、产品说明、报告模板、业务规则
     */
    private String source;

    /**
     * 知识正文
     */
    private String content;

    /**
     * 检索关键词
     */
    private List<String> keywords;

    /**
     * 优先级，数字越大越优先
     */
    private Integer priority;
}