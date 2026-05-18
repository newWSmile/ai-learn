package com.example.ailearn.model.dto.rq;

import lombok.Data;

/**
 * 统一AI助手请求
 */
@Data
public class AssistantChatRequest {

    /**
     * 用户问题
     */
    private String question;
}
