package com.example.ailearn.model.dto.rp;

import lombok.Data;

import java.util.List;

@Data
public class RiskAnalysisResult {

    /**
     * 总体判断
     */
    private String overallJudgement;

    /**
     * 风险等级：LOW / MEDIUM / HIGH / UNKNOWN
     */
    private String riskLevel;

    /**
     * 主要问题
     */
    private List<String> mainProblems;

    /**
     * 风险分析
     */
    private String riskAnalysis;

    /**
     * 整改建议
     */
    private List<String> suggestions;

    /**
     * 是否需要督办
     */
    private Boolean needSupervision;

    /**
     * 数据是否充足
     */
    private Boolean dataEnough;

    /**
     * 缺失字段
     */
    private List<String> missingFields;

}
