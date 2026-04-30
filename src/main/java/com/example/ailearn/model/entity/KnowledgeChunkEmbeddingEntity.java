package com.example.ailearn.model.entity;

import lombok.Data;

/**
 * 知识片段Embedding向量实体
 *
 * 对应表：knowledge_chunk_embedding
 */
@Data
public class KnowledgeChunkEmbeddingEntity {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 知识片段ID，对应 knowledge_chunk.id
     */
    private String knowledgeId;

    /**
     * Embedding模型名称，例如 text-embedding-v4
     */
    private String modelName;

    /**
     * 向量维度，例如 1024
     */
    private Integer dimension;

    /**
     * 知识向量化文本的哈希值
     */
    private String contentHash;

    /**
     * Embedding向量JSON字符串
     */
    private String embeddingVector;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 修改时间
     */
    private String gmtModified;
}
