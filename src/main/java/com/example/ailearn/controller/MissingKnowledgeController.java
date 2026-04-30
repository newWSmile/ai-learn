package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.vo.MissingKnowledgeItemVO;
import com.example.ailearn.service.MissingKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 知识库补全复盘接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/rag/missing-knowledge")
public class MissingKnowledgeController {

    private final MissingKnowledgeService missingKnowledgeService;

    /**
     * 查询最近的知识库未命中问题
     *
     * 示例：
     * GET /ai/rag/missing-knowledge/recent?limit=20
     */
    @GetMapping("/recent")
    public ApiResult<List<MissingKnowledgeItemVO>> recent(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<MissingKnowledgeItemVO> result = missingKnowledgeService.listRecent(limit);
        return ApiResult.success(result);
    }
}