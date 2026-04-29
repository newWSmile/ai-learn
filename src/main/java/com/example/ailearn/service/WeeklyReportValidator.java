package com.example.ailearn.service;


import com.example.ailearn.model.dto.rp.WeeklyReportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WeeklyReportValidator {

    public WeeklyReportResult validateAndFix(WeeklyReportResult result) {
        if (result == null) {
            return buildFallback("AI 未返回有效周报结果");
        }

        if (!StringUtils.hasText(result.getTitle())) {
            result.setTitle("明厨亮灶本周运行周报");
        }

        if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary("当前数据不足，无法生成完整周报摘要");
        }

        if (!StringUtils.hasText(result.getRiskJudgement())) {
            result.setRiskJudgement("当前数据不足，无法完整判断风险水平");
        }

        if (result.getKeyFindings() == null) {
            result.setKeyFindings(new ArrayList<>());
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

        if (result.getDataEnough() == null) {
            result.setDataEnough(false);
        }

        if (Boolean.FALSE.equals(result.getDataEnough()) && result.getMissingFields().isEmpty()) {
            result.setMissingFields(List.of("关键周报数据不足"));
        }

        if (result.getKeyFindings().size() > 5) {
            result.setKeyFindings(new ArrayList<>(result.getKeyFindings().subList(0, 5)));
        }

        if (result.getMainProblems().size() > 5) {
            result.setMainProblems(new ArrayList<>(result.getMainProblems().subList(0, 5)));
        }

        if (result.getSuggestions().size() > 5) {
            result.setSuggestions(new ArrayList<>(result.getSuggestions().subList(0, 5)));
        }

        return result;
    }

    public WeeklyReportResult buildFallback(String reason) {
        WeeklyReportResult fallback = new WeeklyReportResult();
        fallback.setTitle("明厨亮灶本周运行周报");
        fallback.setSummary("当前数据不足，无法生成完整周报");
        fallback.setKeyFindings(List.of());
        fallback.setMainProblems(List.of());
        fallback.setRiskJudgement(reason);
        fallback.setSuggestions(List.of(
                "请补充完整周报统计数据后重新生成",
                "建议至少提供接入学校数、预警总数、处置完成率等核心指标",
                "如需形成风险判断，请补充重复预警、设备离线、台账或票证异常等数据"
        ));
        fallback.setDataEnough(false);
        fallback.setMissingFields(List.of("关键周报数据不足"));
        return fallback;
    }
}