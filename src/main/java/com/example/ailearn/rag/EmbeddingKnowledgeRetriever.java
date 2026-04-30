package com.example.ailearn.rag;


import com.example.ailearn.repository.DatabaseKnowledgeRepository;
import com.example.ailearn.utils.VectorUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingKnowledgeRetriever {


    private final EmbeddingModel embeddingModel;

    /**
     * 内存中的知识向量
     */
    private final List<EmbeddedKnowledgeChunk> embeddedChunks = new ArrayList<>();

    private static final double MIN_SCORE = 0.60D;

    private static final int TOP_K = 2;

    private final DatabaseKnowledgeRepository databaseKnowledgeRepository;


    @PostConstruct
    public void init() {
        log.info("开始初始化本地知识库向量");
        reload();
    }

    /**
     * 重新加载知识向量
     * <p>
     * 说明：
     * Day 22 先在启动时调用。
     * Day 23 做知识库管理接口后，可以在新增或修改知识后手动调用 reload。
     */
    public synchronized void reload() {
        embeddedChunks.clear();

        List<KnowledgeChunk> chunks = databaseKnowledgeRepository.listEnabledKnowledgeChunks();

        if (chunks == null || chunks.isEmpty()) {
            log.warn("Embedding知识向量初始化跳过：数据库中暂无启用知识片段");
            return;
        }

        for (KnowledgeChunk chunk : chunks) {
            try {
                // 用标题 + 内容 + 关键词一起生成向量，提高语义覆盖
                String embeddingText = buildEmbeddingText(chunk);

                float[] vector = embeddingModel.embed(embeddingText);

                EmbeddedKnowledgeChunk embeddedChunk = new EmbeddedKnowledgeChunk();
                embeddedChunk.setChunk(chunk);
                embeddedChunk.setVector(vector);

                embeddedChunks.add(embeddedChunk);
            } catch (Exception e) {
                log.error("知识片段生成Embedding失败，chunkId={}，title={}", chunk.getId(), chunk.getTitle(), e);
            }
        }

        log.info("Embedding知识向量初始化完成，count={}", embeddedChunks.size());
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