package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.KnowledgeChunkEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识片段 Mapper
 */
@Mapper
public interface KnowledgeChunkMapper {

    /**
     * 查询所有启用的知识片段
     *
     * @return 启用中的知识片段
     */
    List<KnowledgeChunkEntity> selectEnabledList();
}