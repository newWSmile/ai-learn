package com.example.ailearn.rag;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 混合检索命中结果
 *
 * 说明：
 * 1. keywordHit 表示是否被关键词检索命中
 * 2. embeddingHit 表示是否被 Embedding 语义检索命中
 * 3. finalScore 是最终排序分数
 * 4. reasons 用来记录为什么这条知识被加分，方便后续排查
 */
@Data
public class HybridKnowledgeMatch {

    /**
     * 知识片段ID
     */
    private String id;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 知识分类
     */
    private String category;

    /**
     * 知识来源
     */
    private String source;

    /**
     * 知识正文
     */
    private String content;

    /**
     * 是否被关键词检索命中
     */
    private Boolean keywordHit = false;

    /**
     * 是否被 Embedding 检索命中
     */
    private Boolean embeddingHit = false;

    /**
     * 关键词检索得分
     */
    private Double keywordScore = 0D;

    /**
     * Embedding 相似度得分
     */
    private Double embeddingScore = 0D;

    /**
     * 业务规则加权分
     */
    private Double ruleScore = 0D;

    /**
     * 最终排序分数
     */
    private Double finalScore = 0D;

    /**
     * 命中原因
     */
    private List<String> reasons = new ArrayList<>();
}