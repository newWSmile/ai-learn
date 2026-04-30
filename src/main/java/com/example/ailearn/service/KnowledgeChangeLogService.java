package com.example.ailearn.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.KnowledgeChangeOperationType;
import com.example.ailearn.mapper.KnowledgeChunkChangeLogMapper;
import com.example.ailearn.model.entity.KnowledgeChunkChangeLogEntity;
import com.example.ailearn.model.vo.KnowledgeChangeLogVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识片段变更日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChangeLogService {

    private static final int DEFAULT_LIMIT = 50;

    private static final int MAX_LIMIT = 200;

    private static final String DEFAULT_OPERATOR = "SYSTEM";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KnowledgeChunkChangeLogMapper knowledgeChunkChangeLogMapper;

    private final ObjectMapper objectMapper;

    /**
     * 记录知识变更日志
     *
     * @param knowledgeId   知识片段ID
     * @param operationType 操作类型
     * @param beforeData    变更前数据
     * @param afterData     变更后数据
     * @param remark        变更说明
     */
    public void record(String knowledgeId,
                       KnowledgeChangeOperationType operationType,
                       Object beforeData,
                       Object afterData,
                       String remark) {
        try {
            KnowledgeChunkChangeLogEntity entity = new KnowledgeChunkChangeLogEntity();
            entity.setId(IdUtil.getSnowflakeNextIdStr());
            entity.setKnowledgeId(knowledgeId);
            entity.setOperationType(operationType.name());
            entity.setBeforeData(toJsonSafely(beforeData));
            entity.setAfterData(toJsonSafely(afterData));
            entity.setRemark(remark);
            entity.setOperator(DEFAULT_OPERATOR);
            entity.setGmtCreate(now());

            knowledgeChunkChangeLogMapper.insert(entity);

            log.info("记录知识变更日志成功，knowledgeId={}，operationType={}，remark={}",
                    knowledgeId, operationType.name(), remark);
        } catch (Exception e) {
            // 变更日志失败不能影响主流程
            log.error("记录知识变更日志失败，knowledgeId={}，operationType={}",
                    knowledgeId, operationType.name(), e);
        }
    }

    /**
     * 查询某条知识的变更日志
     */
    public List<KnowledgeChangeLogVO> listByKnowledgeId(String knowledgeId, Integer limit) {
        if (StrUtil.isBlank(knowledgeId)) {
            throw new IllegalArgumentException("知识片段ID不能为空");
        }

        int safeLimit = normalizeLimit(limit);

        List<KnowledgeChunkChangeLogEntity> list =
                knowledgeChunkChangeLogMapper.selectByKnowledgeId(knowledgeId, safeLimit);

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询变更日志详情
     */
    public KnowledgeChangeLogVO detail(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("日志ID不能为空");
        }

        KnowledgeChunkChangeLogEntity entity = knowledgeChunkChangeLogMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("变更日志不存在：" + id);
        }

        return convertToVO(entity);
    }

    private KnowledgeChangeLogVO convertToVO(KnowledgeChunkChangeLogEntity entity) {
        KnowledgeChangeLogVO vo = new KnowledgeChangeLogVO();
        vo.setId(entity.getId());
        vo.setKnowledgeId(entity.getKnowledgeId());
        vo.setOperationType(entity.getOperationType());
        vo.setBeforeData(entity.getBeforeData());
        vo.setAfterData(entity.getAfterData());
        vo.setRemark(entity.getRemark());
        vo.setOperator(entity.getOperator());
        vo.setGmtCreate(entity.getGmtCreate());
        return vo;
    }

    /**
     * 安全转JSON
     *
     * 说明：
     * null 返回 null，这样新增时 beforeData 可以为空。
     */
    private String toJsonSafely(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("知识变更数据转JSON失败，objectClass={}", object.getClass().getName(), e);
            return "{}";
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}