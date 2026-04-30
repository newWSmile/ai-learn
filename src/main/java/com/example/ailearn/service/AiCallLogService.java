package com.example.ailearn.service;

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

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AiCallLogService(AiCallLogMapper aiCallLogMapper) {
        this.aiCallLogMapper = aiCallLogMapper;
    }

    public void record(AiCallLogRecord record) {
        if (record == null) {
            return;
        }

        log.info("AI调用日志，bizType={}, modelName={}, success={}, costMs={}, needReview={}, userInputLength={}, promptLength={}, responseLength={}, errorMessage={}",
                record.getBizType(),
                record.getModelName(),
                record.getSuccess(),
                record.getCostMs(),
                record.getNeedReview(),
                length(record.getUserInput()),
                length(record.getPrompt()),
                length(record.getResponseText()),
                record.getErrorMessage());


        try {
            AiCallLogEntity entity = convertToEntity(record);
            aiCallLogMapper.insert(entity);
        } catch (Exception e) {
            // 注意：AI调用日志入库失败，不应该影响主业务接口返回
            log.error("AI调用日志入库失败，bizType={}, modelName={}, success={}",
                    record.getBizType(),
                    record.getModelName(),
                    record.getSuccess(),
                    e);
        }
    }

    private int length(String text) {
        return text == null ? 0 : text.length();
    }


    private AiCallLogEntity convertToEntity(AiCallLogRecord record) {
        AiCallLogEntity entity = new AiCallLogEntity();
        entity.setId(SnowflakeIdUtil.nextIdStr());
        entity.setBizType(record.getBizType());
        entity.setModelName(record.getModelName());
        entity.setUserInput(record.getUserInput());
        entity.setPrompt(record.getPrompt());
        entity.setResponseText(record.getResponseText());
        entity.setFinalResult(record.getFinalResult());
        entity.setSuccess(Boolean.TRUE.equals(record.getSuccess()) ? 1 : 0);
        entity.setErrorMessage(record.getErrorMessage());
        entity.setCostMs(record.getCostMs());
        entity.setNeedReview(Boolean.TRUE.equals(record.getNeedReview()) ? 1 : 0);
        entity.setGmtCreate(formatDateTime(record.getCreateTime()));
        return entity;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        LocalDateTime actualDateTime = dateTime == null ? LocalDateTime.now() : dateTime;
        return actualDateTime.format(DATE_TIME_FORMATTER);
    }

}
