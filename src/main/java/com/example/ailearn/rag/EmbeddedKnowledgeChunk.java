package com.example.ailearn.rag;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedKnowledgeChunk {

    private KnowledgeChunk chunk;

    private float[] vector;
}