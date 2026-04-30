package com.example.ailearn.model.dto.rq;

import com.example.ailearn.enums.AiBizType;
import lombok.Data;

@Data
public class AiCallLogQueryRequest {

    /**
     * 业务类型：
     * CHAT / RISK_ANALYSIS / WEEKLY_REPORT / RAG_CHAT
     */
    private AiBizType bizType;

    /**
     * 是否调用成功
     */
    private Boolean success;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;

    /**
     * 查询条数，默认 20
     */
    private Integer limit = 20;

}
