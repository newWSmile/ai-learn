package com.example.ailearn.model.dto.rp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingTestResult {

    /**
     * 嵌入向量的维度
     */
    private Integer dimension;

    /**
     * 嵌入向量的预览
     */
    private String preview;

    /**
     * 是否成功
     */
    private Boolean success;
}