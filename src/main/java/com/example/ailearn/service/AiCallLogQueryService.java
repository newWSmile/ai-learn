package com.example.ailearn.service;

import com.example.ailearn.mapper.AiCallLogMapper;
import com.example.ailearn.model.dto.rq.AiCallLogQueryRequest;
import com.example.ailearn.model.entity.AiCallLogEntity;
import com.example.ailearn.model.vo.AiCallLogDetailVO;
import com.example.ailearn.model.vo.AiCallLogListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallLogQueryService {

    private final AiCallLogMapper aiCallLogMapper;

    public List<AiCallLogListVO> recent(AiCallLogQueryRequest request) {
        Integer limit = normalizeLimit(request.getLimit());

        String bizType = request.getBizType() == null ? null : request.getBizType().name();
        Integer success = toIntegerNullable(request.getSuccess());
        Integer needReview = toIntegerNullable(request.getNeedReview());

        log.info("查询AI调用日志列表, bizType={}, success={}, needReview={}, limit={}",
                bizType, success, needReview, limit);

        List<AiCallLogEntity> entities = aiCallLogMapper.selectRecent(
                bizType,
                success,
                needReview,
                limit
        );

        return entities.stream()
                .map(this::toListVO)
                .toList();
    }

    public AiCallLogDetailVO detail(String id) {
        log.info("查询AI调用日志详情, id={}", id);

        AiCallLogEntity entity = aiCallLogMapper.selectById(id);
        if (entity == null) {
            return null;
        }

        return toDetailVO(entity);
    }

    private Integer toIntegerNullable(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        if (limit > 100) {
            return 100;
        }
        return limit;
    }


    private Boolean toBoolean(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1;
    }

    private AiCallLogListVO toListVO(AiCallLogEntity entity) {
        return AiCallLogListVO.builder()
                .id(entity.getId())
                .bizType(entity.getBizType())
                .modelName(entity.getModelName())
                .userInput(abbreviate(entity.getUserInput(), 120))
                .success(toBoolean(entity.getSuccess()))
                .errorMessage(entity.getErrorMessage())
                .costMs(entity.getCostMs())
                .needReview(toBoolean(entity.getNeedReview()))
                .gmtCreate(entity.getGmtCreate())
                .build();
    }

    private AiCallLogDetailVO toDetailVO(AiCallLogEntity entity) {
        return AiCallLogDetailVO.builder()
                .id(entity.getId())
                .bizType(entity.getBizType())
                .modelName(entity.getModelName())
                .userInput(entity.getUserInput())
                .prompt(entity.getPrompt())
                .responseText(entity.getResponseText())
                .finalResult(entity.getFinalResult())
                .success(toBoolean(entity.getSuccess()))
                .errorMessage(entity.getErrorMessage())
                .costMs(entity.getCostMs())
                .needReview(toBoolean(entity.getNeedReview()))
                .gmtCreate(entity.getGmtCreate())
                .build();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

}
