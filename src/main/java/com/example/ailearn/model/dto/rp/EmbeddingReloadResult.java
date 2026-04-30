package com.example.ailearn.model.dto.rp;

import lombok.Data;

/**
 * Embedding向量刷新结果
 */
@Data
public class EmbeddingReloadResult {

    /**
     * 启用知识数量
     */
    private Integer knowledgeCount = 0;

    /**
     * 最终加载到内存的向量数量
     */
    private Integer loadedCount = 0;

    /**
     * 复用数据库已有向量数量
     */
    private Integer reusedCount = 0;

    /**
     * 新生成向量数量
     */
    private Integer generatedCount = 0;

    /**
     * 失败数量
     */
    private Integer failedCount = 0;
}