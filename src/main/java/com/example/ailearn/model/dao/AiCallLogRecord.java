package com.example.ailearn.model.dao;

import java.time.LocalDateTime;

public class AiCallLogRecord {

    /**
     * 业务类型：CHAT / RISK_ANALYSIS / WEEKLY_REPORT
     */
    private String bizType;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 用户原始输入
     */
    private String userInput;

    /**
     * 最终发送给模型的 Prompt
     */
    private String prompt;

    /**
     * 模型原始响应
     */
    private String responseText;

    /**
     * 后端校验修正后的最终结果
     */
    private String finalResult;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 调用耗时，单位毫秒
     */
    private Long costMs;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public Boolean getNeedReview() {
        return needReview;
    }

    public void setNeedReview(Boolean needReview) {
        this.needReview = needReview;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

}
