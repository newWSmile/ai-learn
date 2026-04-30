package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.KnowledgeChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识片段 Mapper
 */
@Mapper
public interface KnowledgeChunkMapper {

    /**
     * 查询所有启用的知识片段
     */
    List<KnowledgeChunkEntity> selectEnabledList();

    /**
     * 查询知识列表
     */
    List<KnowledgeChunkEntity> selectList(@Param("category") String category,
                                          @Param("enabled") Integer enabled,
                                          @Param("keyword") String keyword,
                                          @Param("limit") Integer limit);

    /**
     * 根据ID查询知识详情
     */
    KnowledgeChunkEntity selectById(@Param("id") String id);

    /**
     * 新增知识片段
     */
    int insert(KnowledgeChunkEntity entity);

    /**
     * 修改知识片段
     */
    int updateById(KnowledgeChunkEntity entity);

    /**
     * 修改启用状态
     */
    int updateEnabled(@Param("id") String id,
                      @Param("enabled") Integer enabled,
                      @Param("gmtModified") String gmtModified);
}