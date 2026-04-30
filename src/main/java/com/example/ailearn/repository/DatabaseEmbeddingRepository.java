package com.example.ailearn.repository;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.example.ailearn.mapper.KnowledgeChunkEmbeddingMapper;
import com.example.ailearn.model.entity.KnowledgeChunkEmbeddingEntity;
import com.example.ailearn.model.vo.StoredEmbeddingVector;
import com.example.ailearn.rag.KnowledgeChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * 数据库Embedding向量仓储
 *
 * 作用：
 * 1. 优先复用数据库中的Embedding向量；
 * 2. 知识内容未变化时不重新调用Embedding模型；
 * 3. 知识内容变化时重新生成向量并更新数据库。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseEmbeddingRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KnowledgeChunkEmbeddingMapper knowledgeChunkEmbeddingMapper;

    private final ObjectMapper objectMapper;

    /**
     * 当前Embedding模型名称
     */
    @Value("${spring.ai.openai.embedding.options.model:text-embedding-v4}")
    private String embeddingModelName;

    /**
     * 当前Embedding维度
     */
    @Value("${spring.ai.openai.embedding.options.dimensions:1024}")
    private Integer embeddingDimension;

    /**
     * 加载或生成向量
     *
     * @param chunk          知识片段
     * @param embeddingText  实际用于向量化的文本
     * @param vectorSupplier 向量生成函数，只有在需要新生成时才会调用
     */
    public StoredEmbeddingVector loadOrCreateVector(KnowledgeChunk chunk,
                                                    String embeddingText,
                                                    Supplier<float[]> vectorSupplier) {
        String contentHash = SecureUtil.sha256(embeddingText);

        KnowledgeChunkEmbeddingEntity exist =
                knowledgeChunkEmbeddingMapper.selectByKnowledgeIdAndModel(chunk.getId(), embeddingModelName);

        if (canReuse(exist, contentHash)) {
            StoredEmbeddingVector result = new StoredEmbeddingVector();
            result.setVector(parseVector(exist.getEmbeddingVector()));
            result.setReused(true);

            log.info("复用知识Embedding向量，knowledgeId={}，title={}", chunk.getId(), chunk.getTitle());
            return result;
        }

        // 只有这里才会真正调用Embedding模型
        float[] vector = vectorSupplier.get();

        saveOrUpdate(chunk, contentHash, vector, exist);

        StoredEmbeddingVector result = new StoredEmbeddingVector();
        result.setVector(vector);
        result.setReused(false);

        log.info("生成并保存知识Embedding向量，knowledgeId={}，title={}，dimension={}",
                chunk.getId(), chunk.getTitle(), vector.length);

        return result;
    }

    /**
     * 判断是否可以复用已有向量
     */
    private boolean canReuse(KnowledgeChunkEmbeddingEntity exist, String contentHash) {
        return exist != null
                && StrUtil.isNotBlank(exist.getEmbeddingVector())
                && contentHash.equals(exist.getContentHash());
    }

    /**
     * 保存或更新向量
     */
    private void saveOrUpdate(KnowledgeChunk chunk,
                              String contentHash,
                              float[] vector,
                              KnowledgeChunkEmbeddingEntity exist) {
        String now = now();

        KnowledgeChunkEmbeddingEntity entity = new KnowledgeChunkEmbeddingEntity();
        entity.setKnowledgeId(chunk.getId());
        entity.setModelName(embeddingModelName);
        entity.setDimension(vector.length);
        entity.setContentHash(contentHash);
        entity.setEmbeddingVector(toJson(vector));
        entity.setGmtModified(now);

        if (exist == null) {
            entity.setId(IdUtil.getSnowflakeNextIdStr());
            entity.setGmtCreate(now);
            knowledgeChunkEmbeddingMapper.insert(entity);
        } else {
            knowledgeChunkEmbeddingMapper.updateByKnowledgeIdAndModel(entity);
        }
    }

    /**
     * 向量转JSON字符串
     */
    private String toJson(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            log.error("Embedding向量转JSON失败", e);
            throw new IllegalStateException("Embedding向量转JSON失败", e);
        }
    }

    /**
     * JSON字符串转向量
     */
    private float[] parseVector(String vectorJson) {
        try {
            return objectMapper.readValue(vectorJson, float[].class);
        } catch (Exception e) {
            log.error("Embedding向量JSON解析失败", e);
            throw new IllegalStateException("Embedding向量JSON解析失败", e);
        }
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}