package com.example.ailearn.rag;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddedKnowledgeChunk {

    private KnowledgeChunk chunk;

    private float[] vector;
}