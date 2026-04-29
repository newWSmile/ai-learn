package com.example.ailearn.service;

import com.example.ailearn.enums.RiskLevel;
import com.example.ailearn.model.dto.rp.RiskAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RiskAnalysisValidator {

    public RiskAnalysisResult validateAndFix(RiskAnalysisResult result) {
        if (result == null) {
            return buildFallback("AI 未返回有效风险分析结果");
        }

        if (!RiskLevel.isValid(result.getRiskLevel())) {
            log.warn("AI返回非法风险等级，已修正为 UNKNOWN，原始值：{}", result.getRiskLevel());
            result.setRiskLevel(RiskLevel.UNKNOWN.name());
        }

        if (result.getDataEnough() == null) {
            result.setDataEnough(false);
        }

        if (result.getNeedSupervision() == null) {
            result.setNeedSupervision(false);
        }

        if (result.getMainProblems() == null) {
            result.setMainProblems(new ArrayList<>());
        }

        if (result.getSuggestions() == null) {
            result.setSuggestions(new ArrayList<>());
        }

        if (result.getMissingFields() == null) {
            result.setMissingFields(new ArrayList<>());
        }

        if (!StringUtils.hasText(result.getOverallJudgement())) {
            result.setOverallJudgement("当前数据不足，无法完整判断风险水平");
        }

        if (!StringUtils.hasText(result.getRiskAnalysis())) {
            result.setRiskAnalysis("当前数据不足，无法判断");
        }

        // dataEnough=false 时，强制保守处理
        if (Boolean.FALSE.equals(result.getDataEnough())) {
            result.setRiskLevel(RiskLevel.UNKNOWN.name());
            result.setNeedSupervision(false);

            if (result.getMissingFields().isEmpty()) {
                result.setMissingFields(List.of("关键分析数据不足"));
            }
        }

        // dataEnough=true 但 riskLevel=UNKNOWN，属于矛盾结果，修正为数据不足
        if (Boolean.TRUE.equals(result.getDataEnough())
                && RiskLevel.UNKNOWN.name().equals(result.getRiskLevel())) {
            log.warn("AI返回结果存在矛盾：dataEnough=true 但 riskLevel=UNKNOWN，已修正为 dataEnough=false");
            result.setDataEnough(false);
            result.setNeedSupervision(false);

            if (result.getMissingFields().isEmpty()) {
                result.setMissingFields(List.of("风险等级判断依据不足"));
            }
        }

        if (result.getMainProblems().size() > 3) {
            result.setMainProblems(new ArrayList<>(result.getMainProblems().subList(0, 3)));
        }

        if (result.getSuggestions().size() > 3) {
            result.setSuggestions(new ArrayList<>(result.getSuggestions().subList(0, 3)));
        }

        return result;
    }

    public RiskAnalysisResult buildFallback(String reason) {
        RiskAnalysisResult fallback = new RiskAnalysisResult();
        fallback.setOverallJudgement("当前数据不足，无法完整判断风险水平");
        fallback.setRiskLevel(RiskLevel.UNKNOWN.name());
        fallback.setMainProblems(List.of());
        fallback.setRiskAnalysis(reason);
        fallback.setSuggestions(List.of(
                "请补充完整监管数据后重新分析",
                "请至少提供预警类型、预警数量、统计周期等基础信息",
                "如需判断风险等级，请补充处置情况、重复预警或设备状态等辅助数据"
        ));
        fallback.setNeedSupervision(false);
        fallback.setDataEnough(false);
        fallback.setMissingFields(List.of("关键分析数据不足"));
        return fallback;
    }
}
