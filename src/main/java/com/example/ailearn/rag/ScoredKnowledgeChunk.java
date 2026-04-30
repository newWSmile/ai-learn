package com.example.ailearn.rag;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoredKnowledgeChunk {

    private KnowledgeChunk chunk;

    private Double score;
}