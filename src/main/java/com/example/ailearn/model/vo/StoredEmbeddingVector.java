package com.example.ailearn.model.vo;

import lombok.Data;

/**
 * 已加载的Embedding向量
 */
@Data
public class StoredEmbeddingVector {

    /**
     * 向量数据
     */
    private float[] vector;

    /**
     * 是否复用数据库已有向量
     *
     * true：数据库已有，且contentHash一致，直接复用
     * false：重新调用Embedding模型生成
     */
    private Boolean reused;
}
