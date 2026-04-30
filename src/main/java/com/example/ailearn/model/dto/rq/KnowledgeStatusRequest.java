package com.example.ailearn.model.dto.rq;

import lombok.Data;

/**
 * 知识启用 / 禁用请求
 */
@Data
public class KnowledgeStatusRequest {

    /**
     * 知识片段ID
     */
    private String id;
}
