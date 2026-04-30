package com.example.ailearn.model.dto.rp;


import com.example.ailearn.model.vo.EmbeddingMatchItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmbeddingRetrieveResult {

    private Boolean knowledgeHit;

    private List<String> matchedKnowledgeTitles;

    private List<String> matchedKnowledgeSources;

    private List<EmbeddingMatchItem> matches;
}