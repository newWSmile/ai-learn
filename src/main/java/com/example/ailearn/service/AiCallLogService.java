package com.example.ailearn.service;

import cn.hutool.core.util.IdUtil;
import com.example.ailearn.mapper.AiCallLogMapper;
import com.example.ailearn.model.dao.AiCallLogRecord;
import com.example.ailearn.model.entity.AiCallLogEntity;
import com.example.ailearn.utils.SnowflakeIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class AiCallLogService {

    private final AiCallLogMapper aiCallLogMapper;

    private final AiNeedReviewDetector aiNeedReviewDetector;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AiCallLogService(AiCallLogMapper aiCallLogMapper, AiNeedReviewDetector aiNeedReviewDetector) {
        this.aiCallLogMapper = aiCallLogMapper;
        this.aiNeedReviewDetector = aiNeedReviewDetector;
    }

    public void record(AiCallLogRecord record) {
        try {
            boolean needReview = aiNeedReviewDetector.detect(
                    record.getBizType(),
                    record.getNeedReview(),
                    record.getResponseText(),
                    record.getFinalResult()
            );

            log.info("记录AI调用日志, bizType={}, success={}, costMs={}, needReview={}",
                    record.getBizType(), record.getSuccess(), record.getCostMs(), needReview);

            AiCallLogEntity entity = convertToEntity(record, needReview);
            entity.setGmtCreate(formatDateTime(LocalDateTime.now()));
            aiCallLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("AI调用日志入库失败, bizType={}", record.getBizType(), e);
        }
    }


    private AiCallLogEntity convertToEntity(AiCallLogRecord record, boolean needReview) {
        AiCallLogEntity entity = new AiCallLogEntity();

        entity.setId(String.valueOf(IdUtil.getSnowflakeNextId()));
        entity.setBizType(record.getBizType() == null ? null : record.getBizType().name());
        entity.setModelName(record.getModelName());
        entity.setUserInput(record.getUserInput());
        entity.setPrompt(record.getPrompt());
        entity.setResponseText(record.getResponseText());
        entity.setFinalResult(record.getFinalResult());
        entity.setSuccess(toInteger(record.getSuccess()));
        entity.setErrorMessage(record.getErrorMessage());
        entity.setCostMs(record.getCostMs());
        entity.setNeedReview(toInteger(needReview));

        return entity;
    }

    private Integer toInteger(Boolean value) {
        if (value == null) {
            return 0;
        }
        return value ? 1 : 0;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        LocalDateTime actualDateTime = dateTime == null ? LocalDateTime.now() : dateTime;
        return actualDateTime.format(DATE_TIME_FORMATTER);
    }

}
