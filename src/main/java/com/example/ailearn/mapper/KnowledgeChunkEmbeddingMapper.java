package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.KnowledgeChunkEmbeddingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识片段Embedding向量 Mapper
 */
@Mapper
public interface KnowledgeChunkEmbeddingMapper {

    /**
     * 根据知识ID和模型名称查询向量记录
     */
    KnowledgeChunkEmbeddingEntity selectByKnowledgeIdAndModel(@Param("knowledgeId") String knowledgeId,
                                                              @Param("modelName") String modelName);

    /**
     * 新增向量记录
     */
    int insert(KnowledgeChunkEmbeddingEntity entity);

    /**
     * 更新向量记录
     */
    int updateByKnowledgeIdAndModel(KnowledgeChunkEmbeddingEntity entity);
}