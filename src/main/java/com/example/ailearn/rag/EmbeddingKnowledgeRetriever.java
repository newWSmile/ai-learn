package com.example.ailearn.rag;


import com.example.ailearn.utils.VectorUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingKnowledgeRetriever {

    private final LocalKnowledgeBase localKnowledgeBase;

    private final EmbeddingModel embeddingModel;

    private List<EmbeddedKnowledgeChunk> embeddedChunks;

    private static final double MIN_SCORE = 0.60D;

    private static final int TOP_K = 2;

    @PostConstruct
    public void init() {
        log.info("开始初始化本地知识库向量");

        this.embeddedChunks = localKnowledgeBase.listAll().stream()
                .map(chunk -> {
                    String embeddingText = buildEmbeddingText(chunk);
                    float[] vector = embeddingModel.embed(embeddingText);

                    log.info("知识片段向量初始化完成, id={}, title={}, dimension={}",
                            chunk.getId(), chunk.getTitle(), vector.length);

                    return EmbeddedKnowledgeChunk.builder()
                            .chunk(chunk)
                            .vector(vector)
                            .build();
                })
                .toList();

        log.info("本地知识库向量初始化完成, count={}", embeddedChunks.size());
    }

    public List<KnowledgeChunk> retrieve(String question) {
        return retrieveWithScore(question).stream()
                .map(ScoredKnowledgeChunk::getChunk)
                .toList();
    }

    public List<ScoredKnowledgeChunk> retrieveWithScore(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        if (embeddedChunks == null || embeddedChunks.isEmpty()) {
            log.warn("本地知识库向量为空");
            return List.of();
        }

        float[] questionVector = embeddingModel.embed(question);

        List<ScoredKnowledgeChunk> scoredChunks = embeddedChunks.stream()
                .map(embeddedChunk -> ScoredKnowledgeChunk.builder()
                        .chunk(embeddedChunk.getChunk())
                        .score(VectorUtils.cosineSimilarity(questionVector, embeddedChunk.getVector()))
                        .build())
                .filter(scoredChunk -> scoredChunk.getScore() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(ScoredKnowledgeChunk::getScore).reversed())
                .limit(TOP_K)
                .toList();

        log.info("Embedding知识检索完成, question={}, matchedCount={}, matched={}",
                question,
                scoredChunks.size(),
                scoredChunks.stream()
                        .map(item -> item.getChunk().getTitle() + ":" + item.getScore())
                        .toList());

        return scoredChunks;
    }

    private String buildEmbeddingText(KnowledgeChunk chunk) {
        return """
                标题：%s
                分类：%s
                来源：%s
                内容：%s
                关键词：%s
                """.formatted(
                chunk.getTitle(),
                chunk.getCategory(),
                chunk.getSource(),
                chunk.getContent(),
                chunk.getKeywords()
        );
    }

    private record ScoredEmbeddedChunk(KnowledgeChunk chunk, double score) {
    }
}