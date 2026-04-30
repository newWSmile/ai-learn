package com.example.ailearn.service;

import cn.hutool.core.collection.CollUtil;
import com.example.ailearn.mapper.AiCallLogMapper;
import com.example.ailearn.model.vo.MissingKnowledgeItemVO;
import com.example.ailearn.rag.MissingKnowledgeActionSuggester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 待补充知识服务
 *
 * 作用：
 * 从 AI 调用日志中找出知识库未命中的问题，
 * 形成后续需要人工复盘和补充知识库的清单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissingKnowledgeService {

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 100;

    private final AiCallLogMapper aiCallLogMapper;

    private final MissingKnowledgeActionSuggester missingKnowledgeActionSuggester;

    /**
     * 查询最近的待补充知识问题
     */
    public List<MissingKnowledgeItemVO> listRecent(Integer limit) {
        int safeLimit = normalizeLimit(limit);

        List<MissingKnowledgeItemVO> list = aiCallLogMapper.selectRecentMissingKnowledge(safeLimit);

        if (CollUtil.isEmpty(list)) {
            log.info("暂无RAG待补充知识问题，limit={}", safeLimit);
            return new ArrayList<>();
        }

        // 给每条未命中问题补充建议动作
        for (MissingKnowledgeItemVO item : list) {
            item.setSuggestedCategory(missingKnowledgeActionSuggester.suggestCategory(item.getQuestion()));
            item.setSuggestedAction(missingKnowledgeActionSuggester.suggest(item.getQuestion()));
        }

        log.info("查询RAG待补充知识问题完成，limit={}，count={}", safeLimit, list.size());

        return list;
    }

    /**
     * 处理 limit，避免传入过大或非法值
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}