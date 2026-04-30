package com.example.ailearn.model.dto.rp;

import com.example.ailearn.rag.HybridKnowledgeMatch;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 混合检索测试返回结果
 */
@Data
public class HybridRetrieveResult {

    /**
     * 是否命中知识库
     */
    private Boolean knowledgeHit;

    /**
     * 命中的知识标题
     */
    private List<String> matchedKnowledgeTitles = new ArrayList<>();

    /**
     * 命中的知识来源
     */
    private List<String> matchedKnowledgeSources = new ArrayList<>();

    /**
     * 详细命中结果
     */
    private List<HybridKnowledgeMatch> matches = new ArrayList<>();

    public static HybridRetrieveResult fromMatches(List<HybridKnowledgeMatch> matches) {
        HybridRetrieveResult result = new HybridRetrieveResult();
        result.setKnowledgeHit(matches != null && !matches.isEmpty());
        result.setMatches(matches == null ? new ArrayList<>() : matches);

        if (matches != null) {
            result.setMatchedKnowledgeTitles(
                    matches.stream()
                            .map(HybridKnowledgeMatch::getTitle)
                            .distinct()
                            .collect(Collectors.toList())
            );

            result.setMatchedKnowledgeSources(
                    matches.stream()
                            .map(HybridKnowledgeMatch::getSource)
                            .distinct()
                            .collect(Collectors.toList())
            );
        }

        return result;
    }
}
