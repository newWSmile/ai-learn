package com.example.ailearn.model.dto.rq;

import lombok.Data;

/**
 * 混合检索测试请求
 */
@Data
public class HybridRetrieveRequest {

    /**
     * 用户问题
     */
    private String question;
}