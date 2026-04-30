package com.example.ailearn.model.entity;

import lombok.Data;

@Data
public class AiCallLogEntity {

    private String id;

    private String bizType;

    private String modelName;

    private String userInput;

    private String prompt;

    private String responseText;

    private String finalResult;

    /**
     * 是否成功：1成功 0失败
     */
    private Integer success;

    private String errorMessage;

    private Long costMs;

    /**
     * 是否需要人工复核：1是 0否
     */
    private Integer needReview;

    private String gmtCreate;
}