package com.example.ailearn.rag;

import com.example.ailearn.model.dto.rp.EmbeddingReloadResult;
import com.example.ailearn.model.vo.StoredEmbeddingVector;
import com.example.ailearn.repository.DatabaseEmbeddingRepository;
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
import java.util.stream.Collectors;

/**
 * Embedding 知识检索器
 *
 * Day 24 改造：
 * 1. reload时优先复用数据库持久化向量；
 * 2. 只有知识内容变化时才重新生成向量；
 * 3. reload返回复用数、新生成数、失败数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingKnowledgeRetriever {

    private static final double MIN_SCORE = 0.60D;

    private static final int TOP_K = 2;

    private final EmbeddingModel embeddingModel;

    private final DatabaseKnowledgeRepository databaseKnowledgeRepository;

    private final DatabaseEmbeddingRepository databaseEmbeddingRepository;

    /**
     * 内存中的知识向量
     */
    private final List<EmbeddedKnowledgeChunk> embeddedChunks = new ArrayList<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新加载知识向量
     */
    public synchronized EmbeddingReloadResult reload() {
        embeddedChunks.clear();

        EmbeddingReloadResult result = new EmbeddingReloadResult();

        List<KnowledgeChunk> chunks = databaseKnowledgeRepository.listEnabledKnowledgeChunks();
        result.setKnowledgeCount(chunks == null ? 0 : chunks.size());

        if (chunks == null || chunks.isEmpty()) {
            log.warn("Embedding知识向量刷新跳过：数据库中暂无启用知识片段");
            return result;
        }

        for (KnowledgeChunk chunk : chunks) {
            try {
                String embeddingText = buildEmbeddingText(chunk);

                StoredEmbeddingVector storedVector = databaseEmbeddingRepository.loadOrCreateVector(
                        chunk,
                        embeddingText,
                        () -> embeddingModel.embed(embeddingText)
                );

                EmbeddedKnowledgeChunk embeddedChunk = new EmbeddedKnowledgeChunk();
                embeddedChunk.setChunk(chunk);
                embeddedChunk.setVector(storedVector.getVector());

                embeddedChunks.add(embeddedChunk);
                result.setLoadedCount(result.getLoadedCount() + 1);

                if (Boolean.TRUE.equals(storedVector.getReused())) {
                    result.setReusedCount(result.getReusedCount() + 1);
                } else {
                    result.setGeneratedCount(result.getGeneratedCount() + 1);
                }
            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                log.error("知识片段Embedding向量加载失败，chunkId={}，title={}",
                        chunk.getId(), chunk.getTitle(), e);
            }
        }

        log.info("Embedding知识向量刷新完成，knowledgeCount={}，loadedCount={}，reusedCount={}，generatedCount={}，failedCount={}",
                result.getKnowledgeCount(),
                result.getLoadedCount(),
                result.getReusedCount(),
                result.getGeneratedCount(),
                result.getFailedCount());

        return result;
    }

    /**
     * 语义检索
     */
    public List<ScoredKnowledgeChunk> retrieveWithScore(String question) {
        if (question == null || question.trim().isEmpty()) {
            log.warn("Embedding检索失败：question为空");
            return List.of();
        }

        if (embeddedChunks.isEmpty()) {
            log.warn("Embedding检索失败：内存中暂无知识向量");
            return List.of();
        }

        float[] questionVector = embeddingModel.embed(question);

        return embeddedChunks.stream()
                .map(item -> {
                    double score = VectorUtils.cosineSimilarity(questionVector, item.getVector());

                    ScoredKnowledgeChunk scored = new ScoredKnowledgeChunk();
                    scored.setChunk(item.getChunk());
                    scored.setScore(score);
                    return scored;
                })
                .filter(item -> item.getScore() >= MIN_SCORE)
                .sorted(Comparator.comparing(ScoredKnowledgeChunk::getScore).reversed())
                .limit(TOP_K)
                .collect(Collectors.toList());
    }

    /**
     * 构建用于生成Embedding的文本
     */
    private String buildEmbeddingText(KnowledgeChunk chunk) {
        String keywords = chunk.getKeywords() == null ? "" : String.join("，", chunk.getKeywords());

        return """
                标题：%s
                分类：%s
                来源：%s
                关键词：%s
                内容：%s
                """.formatted(
                chunk.getTitle(),
                chunk.getCategory(),
                chunk.getSource(),
                keywords,
                chunk.getContent()
        );
    }
}
