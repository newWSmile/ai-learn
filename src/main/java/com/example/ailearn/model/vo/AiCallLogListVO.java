package com.example.ailearn.model.vo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiCallLogListVO {

    private String id;

    private String bizType;

    private String modelName;

    private String userInput;

    private Boolean success;

    private String errorMessage;

    private Long costMs;

    private Boolean needReview;

    private String gmtCreate;
}