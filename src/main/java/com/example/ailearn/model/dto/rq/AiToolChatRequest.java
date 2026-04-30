package com.example.ailearn.model.dto.rq;

import lombok.Data;

/**
 * AI 工具调用聊天请求
 */
@Data
public class AiToolChatRequest {

    /**
     * 用户问题
     */
    private String question;
}