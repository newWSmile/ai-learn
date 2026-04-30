package com.example.ailearn.rag;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScoredKnowledgeChunk {

    private KnowledgeChunk chunk;

    private Double score;
}