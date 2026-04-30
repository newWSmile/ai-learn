package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.AiCallLogEntity;
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
}
