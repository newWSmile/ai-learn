package com.example.ailearn.service;
import com.example.ailearn.enums.AiBizType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AiNeedReviewDetector {

    /**
     * 偏强判断词：
     * 不是绝对禁止，但出现在监管报告、风险分析、周报中时，建议人工复核。
     */
    private static final List<String> STRONG_JUDGEMENT_WORDS = List.of(
            "管理松懈",
            "责任缺失",
            "整改流于形式",
            "管理混乱",
            "监管盲区",
            "整改反复",
            "根源治理",
            "存在疏漏",
            "落实不到位",
            "问责",
            "处罚",
            "高风险区域",
            "重点监管对象"
    );

    /**
     * 越界统计口径词：
     * 如果输入数据没有提供对应口径，AI 却输出这些词，通常需要人工复核。
     */
    private static final List<String> OUT_OF_SCOPE_STAT_WORDS = List.of(
            "累计",
            "共计",
            "同比",
            "环比",
            "全区",
            "全市",
            "本月",
            "持续上升",
            "持续下降",
            "排名靠前",
            "居前",
            "最高",
            "最低",
            "超过一半",
            "接近一半"
    );

    public boolean detect(String bizType,
                          Boolean currentNeedReview,
                          String responseText,
                          String finalResult) {

        if (Boolean.TRUE.equals(currentNeedReview)) {
            return true;
        }

        if (!needDetectBizType(bizType)) {
            return false;
        }

        String text = chooseText(responseText, finalResult);
        if (text == null || text.isBlank()) {
            return false;
        }

        List<String> hitWords = new ArrayList<>();
        hitWords.addAll(hitWords(text, STRONG_JUDGEMENT_WORDS));
        hitWords.addAll(hitWords(text, OUT_OF_SCOPE_STAT_WORDS));

        if (!hitWords.isEmpty()) {
            log.warn("AI结果触发人工复核规则, bizType={}, hitWords={}", bizType, hitWords);
            return true;
        }

        return false;
    }

    private boolean needDetectBizType(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            return false;
        }

        return "RISK_ANALYSIS".equals(bizType)
                || "WEEKLY_REPORT".equals(bizType);
    }

    private String chooseText(String responseText, String finalResult) {
        if (finalResult != null && !finalResult.isBlank()) {
            return finalResult;
        }
        return responseText;
    }

    private List<String> hitWords(String text, List<String> words) {
        List<String> result = new ArrayList<>();

        for (String word : words) {
            if (text.contains(word)) {
                result.add(word);
            }
        }

        return result;
    }
}