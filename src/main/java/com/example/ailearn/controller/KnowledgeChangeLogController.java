package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.vo.KnowledgeChangeLogVO;
import com.example.ailearn.service.KnowledgeChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识变更日志接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/knowledge/change-log")
public class KnowledgeChangeLogController {

    private final KnowledgeChangeLogService knowledgeChangeLogService;

    /**
     * 查询某条知识的变更日志列表
     */
    @GetMapping("/list")
    public ApiResult<List<KnowledgeChangeLogVO>> list(
            @RequestParam("knowledgeId") String knowledgeId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<KnowledgeChangeLogVO> result =
                knowledgeChangeLogService.listByKnowledgeId(knowledgeId, limit);
        return ApiResult.success(result);
    }

    /**
     * 查询知识变更日志详情
     */
    @GetMapping("/detail")
    public ApiResult<KnowledgeChangeLogVO> detail(@RequestParam("id") String id) {
        KnowledgeChangeLogVO result = knowledgeChangeLogService.detail(id);
        return ApiResult.success(result);
    }
}