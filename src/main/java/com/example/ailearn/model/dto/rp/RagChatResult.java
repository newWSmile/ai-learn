package com.example.ailearn.model.dto.rp;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatResult {

    /**
     * AI回答
     */
    private String answer;

    /**
     * 是否命中知识库
     */
    private Boolean knowledgeHit;

    /**
     * 命中的知识片段标题
     */
    private List<String> matchedKnowledgeTitles;

    /**
     * 命中的知识片段来源
     */
    private List<String> matchedKnowledgeSources;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;
}