package com.example.ailearn.model.dto.rq;


import lombok.Data;

@Data
public class RagChatRequest {

    /**
     * 用户问题
     */
    private String question;
}