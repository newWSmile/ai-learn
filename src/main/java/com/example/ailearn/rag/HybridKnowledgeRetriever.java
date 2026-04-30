package com.example.ailearn.rag;

import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.KnowledgeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 混合知识检索器
 * <p>
 * 核心作用：
 * 1. 同时使用关键词检索和 Embedding 检索；
 * 2. 将两种检索结果合并去重；
 * 3. 对关键词命中的知识片段加权；
 * 4. 根据业务词做简单重排序；
 * 5. 返回最终最适合拼进 RAG Prompt 的知识片段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridKnowledgeRetriever {

    /**
     * 最终返回数量
     * <p>
     * 说明：
     * Day 18 阶段建议先控制在 3 条以内。
     * 否则 Prompt 中塞入太多相近知识，反而容易干扰模型。
     */
    private static final int TOP_K = 3;

    /**
     * 最低最终分数
     * <p>
     * 说明：
     * 分数太低说明只是轻微相关，不建议召回。
     */
    private static final double MIN_FINAL_SCORE = 0.60D;

    /**
     * 关键词命中基础分
     * <p>
     * 说明：
     * 关键词检索通常更精准，因此给一个较高基础分。
     */
    private static final double KEYWORD_HIT_SCORE = 0.80D;

    /**
     * 强业务规则加权
     */
    private static final double STRONG_RULE_BOOST = 0.50D;

    /**
     * 弱业务规则加权
     */
    private static final double WEAK_RULE_BOOST = 0.20D;

    /**
     * 报告表达类知识的辅助加分
     * <p>
     * 说明：
     * 报告表达规则是辅助知识，不应该分数过高。
     */
    private static final double REPORT_STYLE_RULE_BOOST = 0.10D;

    /**
     * 当问题已经命中具体业务问题时，报告表达类知识降权
     * <p>
     * 说明：
     * 避免“报告表达要求”压过“摄像头离线说明”“垃圾桶未盖说明”等主知识。
     */
    private static final double REPORT_STYLE_AUXILIARY_PENALTY = -0.20D;

    private final KnowledgeRetriever knowledgeRetriever;

    private final EmbeddingKnowledgeRetriever embeddingKnowledgeRetriever;

    /**
     * 台账类业务规则加权
     *
     * 说明：
     * 当用户问题明确是晨检、留样、消毒、陪餐等台账类问题时，
     * 台账知识应优先于通用报告表达知识。
     */
    private static final double LEDGER_RULE_BOOST = 0.50D;

    /**
     * 执行混合检索
     *
     * @param question 用户问题
     * @return 混合检索结果
     */
    public List<HybridKnowledgeMatch> retrieve(String question) {
        if (StrUtil.isBlank(question)) {
            log.warn("混合检索失败：用户问题为空");
            return new ArrayList<>();
        }

        Map<String, HybridKnowledgeMatch> matchMap = new LinkedHashMap<>();

        // 1. 关键词检索
        List<KnowledgeChunk> keywordChunks = knowledgeRetriever.retrieve(question);
        for (KnowledgeChunk chunk : keywordChunks) {
            HybridKnowledgeMatch match = getOrCreate(matchMap, chunk);
            match.setKeywordHit(true);
            match.setKeywordScore(KEYWORD_HIT_SCORE);
            match.getReasons().add("关键词检索命中");
        }

        // 2. Embedding 语义检索
        List<ScoredKnowledgeChunk> embeddingChunks = embeddingKnowledgeRetriever.retrieveWithScore(question);
        for (ScoredKnowledgeChunk scoredChunk : embeddingChunks) {
            KnowledgeChunk chunk = scoredChunk.getChunk();

            HybridKnowledgeMatch match = getOrCreate(matchMap, chunk);
            match.setEmbeddingHit(true);
            match.setEmbeddingScore(scoredChunk.getScore());
            match.getReasons().add("Embedding语义检索命中，相似度=" + formatScore(scoredChunk.getScore()));
        }

        // 3. 业务规则加权
        for (HybridKnowledgeMatch match : matchMap.values()) {
            applyBusinessRules(question, match);
            calculateFinalScore(match);
        }

        // 4. 按最终分数倒序排序，并过滤低分结果
        List<HybridKnowledgeMatch> result = matchMap.values()
                .stream()
                .filter(match -> match.getFinalScore() >= MIN_FINAL_SCORE)
                .sorted(Comparator.comparing(HybridKnowledgeMatch::getFinalScore).reversed())
                .limit(TOP_K)
                .collect(Collectors.toList());

        log.info("混合检索完成，question={}，keywordHitCount={}，embeddingHitCount={}，finalHitCount={}",
                question,
                keywordChunks.size(),
                embeddingChunks.size(),
                result.size());

        return result;
    }

    /**
     * 从 Map 中获取已有命中项；如果没有，则创建一个新的命中项。
     */
    private HybridKnowledgeMatch getOrCreate(Map<String, HybridKnowledgeMatch> matchMap, KnowledgeChunk chunk) {
        return matchMap.computeIfAbsent(chunk.getId(), id -> {
            HybridKnowledgeMatch match = new HybridKnowledgeMatch();
            match.setId(chunk.getId());
            match.setTitle(chunk.getTitle());
            match.setCategory(chunk.getCategory());
            match.setSource(chunk.getSource());
            match.setContent(chunk.getContent());
            return match;
        });
    }

    /**
     * 应用业务规则加权
     * <p>
     * 说明：
     * Embedding 只能判断语义相似，但不能完全理解业务边界。
     * 所以这里用简单规则把明显相关的知识片段往前排。
     */
    private void applyBusinessRules(String question, HybridKnowledgeMatch match) {
        String title = defaultString(match.getTitle());
        String content = defaultString(match.getContent());

        // 规则1：用户问“离线 / 掉线 / 无法接入”，优先提高“摄像头离线说明”
        if (containsAny(question, "离线", "掉线", "无法接入", "断开", "不在线")
                && containsAny(title + content, "摄像头离线", "离线", "掉线")) {
            addRuleScore(match, STRONG_RULE_BOOST, "命中离线类业务规则");
        }

        // 规则2：用户问“遮挡 / 挡住 / 看不见 / 画面被挡”，优先提高“摄像头遮挡说明”
        if (containsAny(question, "遮挡", "挡住", "看不见", "画面被挡", "被挡住", "画面异常")
                && containsAny(title + content, "摄像头遮挡", "遮挡", "画面被挡")) {
            addRuleScore(match, STRONG_RULE_BOOST, "命中遮挡类业务规则");
        }

        // 规则3：用户问“垃圾桶未盖”，优先提高“垃圾桶未盖预警说明”
        if (containsAny(question, "垃圾桶", "未盖", "没盖", "垃圾桶未盖")
                && containsAny(title + content, "垃圾桶未盖", "垃圾桶")) {
            addRuleScore(match, STRONG_RULE_BOOST, "命中垃圾桶未盖业务规则");
        }

        // 规则4：用户问“报告 / 怎么写 / 表述”时，报告表达要求可以作为辅助知识
        if (containsAny(question, "报告", "怎么写", "如何写", "表述", "描述", "分析")
                && containsAny(title + content, "报告", "表达", "表述")) {
            addRuleScore(match, REPORT_STYLE_RULE_BOOST, "命中报告表达辅助规则");
        }

        // 规则5：如果问题中已经包含明确业务问题，则报告表达类知识降权
        // 例如：点位掉线、摄像头遮挡、垃圾桶未盖，这些都应该优先返回具体业务知识
        if (isSpecificBusinessQuestion(question) && isReportStyle(match)) {
            addRuleScore(match, REPORT_STYLE_AUXILIARY_PENALTY, "业务问题下报告表达知识作为辅助知识降权");
        }

        // 规则：用户明确问遮挡类问题时，离线类知识降权
        if (containsAny(question, "遮挡", "挡住", "看不见", "画面被挡", "被挡住")
                && containsAny(title + content, "摄像头离线", "离线", "掉线", "无法接入")) {
            addRuleScore(match, -0.50D, "遮挡问题下离线类知识降权");
        }

        // 规则：用户明确问离线类问题时，遮挡类知识降权
        if (containsAny(question, "离线", "掉线", "无法接入", "不在线", "断开")
                && containsAny(title + content, "摄像头遮挡", "遮挡", "画面被挡")) {
            addRuleScore(match, -0.50D, "离线问题下遮挡类知识降权");
        }

        // 规则：用户问晨检、晨午检、留样、消毒、陪餐等台账问题时，提高台账类知识优先级
        if (containsAny(question, "台账", "晨检", "晨午检", "留样", "消毒", "陪餐", "未提交", "没有提交", "未上报", "未填报")
                && KnowledgeCategory.LEDGER_RULE.equals(match.getCategory())) {
            addRuleScore(match, LEDGER_RULE_BOOST, "命中台账类业务规则");
        }
    }

    /**
     * 判断是否是报告表达类知识
     */
    private boolean isReportStyle(HybridKnowledgeMatch match) {
        return KnowledgeCategory.REPORT_STYLE.equals(match.getCategory());
    }

    /**
     * 判断用户问题中是否包含明确业务问题
     *
     * 说明：
     * 如果包含明确业务问题，就应该优先返回具体业务知识；
     * 报告表达要求只能作为辅助知识。
     */
    private boolean isSpecificBusinessQuestion(String question) {
        return containsAny(question,
                "离线", "掉线", "无法接入", "不在线", "断开",
                "遮挡", "挡住", "看不见", "画面被挡", "被挡住",
                "垃圾桶", "未盖", "没盖",
                "未戴帽子", "未戴口罩", "未着工装",
                "抽烟", "鼠患", "火情", "视频时间异常",
                "台账", "晨检", "晨午检", "留样", "消毒", "陪餐", "未提交", "未上报", "未填报"
        );
    }

    /**
     * 计算最终分数
     */
    private void calculateFinalScore(HybridKnowledgeMatch match) {
        double keywordScore = safeDouble(match.getKeywordScore());
        double embeddingScore = safeDouble(match.getEmbeddingScore());
        double ruleScore = safeDouble(match.getRuleScore());

        double finalScore = keywordScore + embeddingScore + ruleScore;

        match.setFinalScore(finalScore);
    }

    /**
     * 增加业务规则分
     */
    private void addRuleScore(HybridKnowledgeMatch match, double score, String reason) {
        match.setRuleScore(safeDouble(match.getRuleScore()) + score);
        match.getReasons().add(reason + "，加分=" + score);
    }

    /**
     * 判断文本中是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }

        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String defaultString(String text) {
        return text == null ? "" : text;
    }

    private double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    private String formatScore(Double score) {
        if (score == null) {
            return "0";
        }
        return String.format("%.4f", score);
    }
}