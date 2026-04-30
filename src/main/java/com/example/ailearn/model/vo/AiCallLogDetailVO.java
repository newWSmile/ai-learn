package com.example.ailearn.model.vo;



import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiCallLogDetailVO {

    private String id;

    private String bizType;

    private String modelName;

    private String userInput;

    private String prompt;

    private String responseText;

    private String finalResult;

    private Boolean success;

    private String errorMessage;

    private Long costMs;

    private Boolean needReview;

    private String gmtCreate;
}