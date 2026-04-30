package com.example.ailearn.rag;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private final LocalKnowledgeBase localKnowledgeBase;

    public List<KnowledgeChunk> retrieve(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        List<KnowledgeChunk> matchedChunks = localKnowledgeBase.listAll().stream()
                .map(chunk -> new ScoredChunk(chunk, score(question, chunk)))
                .filter(scoredChunk -> scoredChunk.score() > 0)
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .limit(3)
                .map(ScoredChunk::chunk)
                .toList();

        log.info("RAG知识检索完成, question={}, matchedCount={}", question, matchedChunks.size());

        return matchedChunks;
    }

    private int score(String question, KnowledgeChunk chunk) {
        int score = 0;

        if (chunk.getTitle() != null && question.contains(chunk.getTitle())) {
            score += 5;
        }

        if (chunk.getKeywords() != null) {
            for (String keyword : chunk.getKeywords()) {
                if (question.contains(keyword)) {
                    score += 3;
                }
            }
        }

        if (chunk.getContent() != null) {
            for (String keyword : chunk.getKeywords()) {
                if (chunk.getContent().contains(keyword) && question.contains(keyword)) {
                    score += 1;
                }
            }
        }

        return score;
    }

    private record ScoredChunk(KnowledgeChunk chunk, int score) {
    }
}