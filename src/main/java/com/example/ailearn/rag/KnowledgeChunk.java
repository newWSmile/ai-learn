package com.example.ailearn.rag;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeChunk {

    /**
     * 知识片段ID
     */
    private String id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 关键词，用于本地检索
     */
    private List<String> keywords;
}