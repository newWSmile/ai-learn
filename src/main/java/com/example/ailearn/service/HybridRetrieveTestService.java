package com.example.ailearn.service;

import cn.hutool.core.util.StrUtil;
import com.example.ailearn.model.dto.rp.HybridRetrieveResult;
import com.example.ailearn.model.dto.rq.HybridRetrieveRequest;
import com.example.ailearn.rag.HybridKnowledgeMatch;
import com.example.ailearn.rag.HybridKnowledgeRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 混合检索测试服务
 *
 * 说明：
 * 这个服务暂时只用于验证混合检索效果。
 * Day 18 不建议直接替换 RagChatService，先用测试接口观察召回是否稳定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrieveTestService {

    private final HybridKnowledgeRetriever hybridKnowledgeRetriever;

    public HybridRetrieveResult retrieve(HybridRetrieveRequest request) {
        if (request == null || StrUtil.isBlank(request.getQuestion())) {
            log.warn("混合检索测试失败：question为空");

            HybridRetrieveResult result = new HybridRetrieveResult();
            result.setKnowledgeHit(false);
            result.setMatches(new ArrayList<>());
            return result;
        }

        String question = request.getQuestion();

        List<HybridKnowledgeMatch> matches = hybridKnowledgeRetriever.retrieve(question);

        log.info("混合检索测试完成，question={}，knowledgeHit={}，matchCount={}",
                question,
                matches != null && !matches.isEmpty(),
                matches == null ? 0 : matches.size());

        return HybridRetrieveResult.fromMatches(matches);
    }
}
