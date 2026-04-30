package com.example.ailearn.service;
import com.example.ailearn.enums.AiBizType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AiNeedReviewDetector {

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

    public boolean detect(AiBizType bizType,
                          Boolean currentNeedReview,
                          String responseText,
                          String finalResult) {

        // 如果业务层已经明确标记需要复核，直接保留 true
        if (Boolean.TRUE.equals(currentNeedReview)) {
            return true;
        }

        // 只对需要强监管表达控制的业务类型做自动检测
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

    private boolean needDetectBizType(AiBizType bizType) {
        if (bizType == null) {
            return false;
        }

        return bizType == AiBizType.RISK_ANALYSIS
                || bizType == AiBizType.WEEKLY_REPORT;
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