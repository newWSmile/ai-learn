package com.example.ailearn.model.vo;

import lombok.Data;

/**
 * RAG 待补充知识项
 *
 * 说明：
 * 这不是知识库本身，而是从 ai_call_log 中筛出来的“未命中问题”。
 */
@Data
public class MissingKnowledgeItemVO {

    /**
     * 日志ID
     */
    private String id;

    /**
     * 用户问题
     */
    private String question;

    /**
     * 模型名称
     *
     * 未命中知识库时通常为 SYSTEM_FALLBACK。
     */
    private String modelName;

    /**
     * 接口耗时，单位毫秒
     */
    private Long costMs;

    /**
     * 是否需要人工复核
     */
    private Boolean needReview;

    /**
     * 创建时间
     */
    private String gmtCreate;

    /**
     * 建议处理动作
     */
    private String suggestedAction;


    /**
     * 建议分类
     *
     * 例如：合同类、台账类、设备运维类、AI预警类、报告生成类、其他。
     */
    private String suggestedCategory;
}