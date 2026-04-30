package com.example.ailearn.service;


import com.example.ailearn.model.dto.rp.EmbeddingRetrieveResult;
import com.example.ailearn.model.dto.rq.RagChatRequest;
import com.example.ailearn.model.vo.EmbeddingMatchItem;
import com.example.ailearn.rag.EmbeddingKnowledgeRetriever;
import com.example.ailearn.rag.KnowledgeChunk;
import com.example.ailearn.rag.ScoredKnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingRetrieveTestService {

    private final EmbeddingKnowledgeRetriever embeddingKnowledgeRetriever;


    public EmbeddingRetrieveResult retrieve(RagChatRequest request) {
        String question = request == null ? null : request.getQuestion();

        List<ScoredKnowledgeChunk> scoredChunks = embeddingKnowledgeRetriever.retrieveWithScore(question);

        return EmbeddingRetrieveResult.builder()
                .knowledgeHit(!scoredChunks.isEmpty())
                .matchedKnowledgeTitles(scoredChunks.stream()
                        .map(item -> item.getChunk().getTitle())
                        .toList())
                .matchedKnowledgeSources(scoredChunks.stream()
                        .map(item -> item.getChunk().getSource())
                        .toList())
                .matches(scoredChunks.stream()
                        .map(item -> EmbeddingMatchItem.builder()
                                .title(item.getChunk().getTitle())
                                .source(item.getChunk().getSource())
                                .score(roundScore(item.getScore()))
                                .build())
                        .toList())
                .build();
    }

    private Double roundScore(Double score) {
        if (score == null) {
            return null;
        }

        return BigDecimal.valueOf(score)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}