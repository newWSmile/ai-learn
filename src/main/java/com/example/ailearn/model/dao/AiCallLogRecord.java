package com.example.ailearn.model.dao;

import com.example.ailearn.enums.AiBizType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCallLogRecord {

    /**
     * 业务类型：
     * CHAT / RISK_ANALYSIS / WEEKLY_REPORT / RAG_CHAT
     */
    private AiBizType bizType;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 用户输入
     */
    private String userInput;

    /**
     * 完整 Prompt
     */
    private String prompt;

    /**
     * AI 原始返回
     */
    private String responseText;

    /**
     * 系统最终结果
     */
    private String finalResult;

    /**
     * 是否调用成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 耗时，毫秒
     */
    private Long costMs;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;
}
