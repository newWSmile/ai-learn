package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.AiCallLogEntity;
import com.example.ailearn.model.vo.MissingKnowledgeItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiCallLogMapper {

    int insert(AiCallLogEntity entity);

    List<AiCallLogEntity> selectRecent(@Param("bizType") String bizType,
                                       @Param("success") Integer success,
                                       @Param("needReview") Integer needReview,
                                       @Param("limit") Integer limit);


    AiCallLogEntity selectById(@Param("id") String id);

    /**
     * 查询最近的 RAG 知识库未命中问题
     *
     * @param limit 查询数量
     * @return 待补充知识问题列表
     */
    List<MissingKnowledgeItemVO> selectRecentMissingKnowledge(@Param("limit") Integer limit);
}
