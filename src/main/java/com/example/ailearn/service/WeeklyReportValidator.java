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

    private static final int MAX_KEY_FINDINGS = 5;
    private static final int MAX_MAIN_PROBLEMS = 3;
    private static final int MAX_SUGGESTIONS = 3;

    private static final List<String> STRONG_JUDGEMENT_WORDS = List.of(
            "管理混乱",
            "责任缺失",
            "整改流于形式",
            "严重失职",
            "高风险区域",
            "重点单位",
            "重点监管对象",
            "持续上升",
            "全区",
            "全市",
            "同比",
            "环比",
            "排名靠前",
            "最高",
            "最低",
            "各校明细",
            "各接入学校明细",
            "各接入学校的预警明细",
            "学校名单"
    );

    public WeeklyReportResult validateAndFix(WeeklyReportResult result) {
        if (result == null) {
            return buildFallback("AI 未返回有效周报结果");
        }

        fixBasicFields(result);
        fixListFields(result);
        fixDataEnoughLogic(result);
        trimListSize(result);
        warnUnsafeWords(result);

        return result;
    }

    private void fixBasicFields(WeeklyReportResult result) {
        if (!StringUtils.hasText(result.getTitle())) {
            result.setTitle("明厨亮灶本周运行周报");
        }

        if (!StringUtils.hasText(result.getSummary())) {
            result.setSummary("当前数据不足，无法生成完整周报");
        }

        if (!StringUtils.hasText(result.getRiskJudgement())) {
            result.setRiskJudgement("当前数据不足，无法完整判断风险水平");
        }

        if (result.getDataEnough() == null) {
            result.setDataEnough(false);
        }
    }

    private void fixListFields(WeeklyReportResult result) {
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
    }

    private void fixDataEnoughLogic(WeeklyReportResult result) {
        if (Boolean.FALSE.equals(result.getDataEnough())) {
            if (!result.getSummary().contains("当前数据不足")) {
                result.setSummary("当前数据不足，无法生成完整周报。" + result.getSummary());
            }

            if (result.getMissingFields().isEmpty()) {
                result.setMissingFields(List.of("关键周报数据不足"));
            }

            if (result.getSuggestions().isEmpty()) {
                result.setSuggestions(List.of(
                        "请补充完整周报统计数据后重新生成",
                        "建议至少提供接入学校数、预警总数、处置完成率等核心指标",
                        "如需形成风险判断，请补充重复预警、设备离线、台账或票证异常等数据"
                ));
            }
        }

        if (Boolean.TRUE.equals(result.getDataEnough()) && !result.getMissingFields().isEmpty()) {
            log.warn("周报结果存在矛盾：dataEnough=true 但 missingFields 不为空，missingFields={}",
                    result.getMissingFields());
        }
    }

    private void trimListSize(WeeklyReportResult result) {
        if (result.getKeyFindings().size() > MAX_KEY_FINDINGS) {
            result.setKeyFindings(new ArrayList<>(result.getKeyFindings().subList(0, MAX_KEY_FINDINGS)));
        }

        if (result.getMainProblems().size() > MAX_MAIN_PROBLEMS) {
            result.setMainProblems(new ArrayList<>(result.getMainProblems().subList(0, MAX_MAIN_PROBLEMS)));
        }

        if (result.getSuggestions().size() > MAX_SUGGESTIONS) {
            result.setSuggestions(new ArrayList<>(result.getSuggestions().subList(0, MAX_SUGGESTIONS)));
        }
    }

    private void warnUnsafeWords(WeeklyReportResult result) {
        String allText = String.join(" ",
                safe(result.getSummary()),
                safe(result.getRiskJudgement()),
                String.join(" ", result.getKeyFindings()),
                String.join(" ", result.getMainProblems()),
                String.join(" ", result.getSuggestions())
        );

        for (String word : STRONG_JUDGEMENT_WORDS) {
            if (allText.contains(word)) {
                log.warn("AI周报结果包含需关注表述：{}，请检查是否存在过度归因或口径越界", word);
            }
        }
    }

    private String safe(String text) {
        return text == null ? "" : text;
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