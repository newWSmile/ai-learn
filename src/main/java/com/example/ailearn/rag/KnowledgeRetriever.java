package com.example.ailearn.rag;


import com.example.ailearn.enums.KnowledgeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private final LocalKnowledgeBase localKnowledgeBase;

    /**
     * 根据问题，从知识库中检索匹配的片段
     *  标题命中：+10
     * 关键词命中：+5
     * 分类命中：+3
     * 同分时按 priority 排序
     * @param question 问题
     * @return 匹配的片段
     */
    public List<KnowledgeChunk> retrieve(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        List<KnowledgeChunk> matchedChunks = localKnowledgeBase.listAll().stream()
                .map(chunk -> new ScoredChunk(chunk, score(question, chunk)))
                .filter(scoredChunk -> scoredChunk.score() > 5)
                .sorted(Comparator
                        .comparingInt(ScoredChunk::score).reversed()
                        .thenComparing(scoredChunk -> safePriority(scoredChunk.chunk()), Comparator.reverseOrder()))
                .limit(3)
                .map(ScoredChunk::chunk)
                .toList();

        log.info("RAG知识检索完成, question={}, matchedCount={}, matchedTitles={}",
                question,
                matchedChunks.size(),
                matchedChunks.stream().map(KnowledgeChunk::getTitle).toList());

        return matchedChunks;
    }

    private int score(String question, KnowledgeChunk chunk) {
        int score = 0;

        if (question == null || question.isBlank() || chunk == null) {
            return score;
        }

        if (chunk.getTitle() != null && question.contains(chunk.getTitle())) {
            score += 20;
        }

        if (chunk.getKeywords() != null) {
            for (String keyword : chunk.getKeywords()) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }

                if (question.contains(keyword)) {
                    score += keyword.length() >= 4 ? 8 : 5;
                }
            }
        }

        // 分类只做轻微加分，不能让它主导命中
        String categoryText = categoryToChinese(chunk.getCategory());
        if (!categoryText.isBlank() && question.contains(categoryText)) {
            score += 2;
        }

        return score;
    }

    private String categoryToChinese(String category) {
        if (KnowledgeCategory.ALARM_TYPE.equals(category)) {
            return "预警类型";
        }
        if (KnowledgeCategory.ALARM_EXPLANATION.equals(category)) {
            return "预警";
        }
        if (KnowledgeCategory.REPORT_STYLE.equals(category)) {
            return "报告";
        }
        if (KnowledgeCategory.DEVICE_OPERATION.equals(category)) {
            return "设备";
        }
        if (KnowledgeCategory.LEDGER_RULE.equals(category)) {
            return "台账";
        }
        return "";
    }

    private Integer safePriority(KnowledgeChunk chunk) {
        return chunk.getPriority() == null ? 0 : chunk.getPriority();
    }

    private record ScoredChunk(KnowledgeChunk chunk, int score) {
    }
}