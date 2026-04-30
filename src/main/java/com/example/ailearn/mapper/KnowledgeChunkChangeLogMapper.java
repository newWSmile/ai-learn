package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.KnowledgeChunkChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识片段变更日志 Mapper
 */
@Mapper
public interface KnowledgeChunkChangeLogMapper {

    /**
     * 新增变更日志
     */
    int insert(KnowledgeChunkChangeLogEntity entity);

    /**
     * 查询某条知识的变更日志
     */
    List<KnowledgeChunkChangeLogEntity> selectByKnowledgeId(@Param("knowledgeId") String knowledgeId,
                                                            @Param("limit") Integer limit);

    /**
     * 根据ID查询变更日志详情
     */
    KnowledgeChunkChangeLogEntity selectById(@Param("id") String id);
}