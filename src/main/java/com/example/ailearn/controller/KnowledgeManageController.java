package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.EmbeddingReloadResult;
import com.example.ailearn.model.dto.rq.KnowledgeCreateRequest;
import com.example.ailearn.model.dto.rq.KnowledgeStatusRequest;
import com.example.ailearn.model.dto.rq.KnowledgeUpdateRequest;
import com.example.ailearn.model.vo.KnowledgeChunkVO;
import com.example.ailearn.service.KnowledgeManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/knowledge")
public class KnowledgeManageController {

    private final KnowledgeManageService knowledgeManageService;

    /**
     * 查询知识列表
     */
    @GetMapping("/list")
    public ApiResult<List<KnowledgeChunkVO>> list(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<KnowledgeChunkVO> result = knowledgeManageService.list(category, enabled, keyword, limit);
        return ApiResult.success(result);
    }

    /**
     * 查询知识详情
     */
    @GetMapping("/detail")
    public ApiResult<KnowledgeChunkVO> detail(@RequestParam("id") String id) {
        KnowledgeChunkVO result = knowledgeManageService.detail(id);
        return ApiResult.success(result);
    }

    /**
     * 新增知识
     */
    @PostMapping("/create")
    public ApiResult<KnowledgeChunkVO> create(@RequestBody KnowledgeCreateRequest request) {
        KnowledgeChunkVO result = knowledgeManageService.create(request);
        return ApiResult.success(result);
    }

    /**
     * 修改知识
     */
    @PostMapping("/update")
    public ApiResult<KnowledgeChunkVO> update(@RequestBody KnowledgeUpdateRequest request) {
        KnowledgeChunkVO result = knowledgeManageService.update(request);
        return ApiResult.success(result);
    }

    /**
     * 启用知识
     */
    @PostMapping("/enable")
    public ApiResult<KnowledgeChunkVO> enable(@RequestBody KnowledgeStatusRequest request) {
        KnowledgeChunkVO result = knowledgeManageService.enable(request.getId());
        return ApiResult.success(result);
    }

    /**
     * 禁用知识
     */
    @PostMapping("/disable")
    public ApiResult<KnowledgeChunkVO> disable(@RequestBody KnowledgeStatusRequest request) {
        KnowledgeChunkVO result = knowledgeManageService.disable(request.getId());
        return ApiResult.success(result);
    }

    /**
     * 手动刷新知识库向量
     */
    @PostMapping("/reload")
    public ApiResult<EmbeddingReloadResult> reload() {
        EmbeddingReloadResult result = knowledgeManageService.reload();
        return ApiResult.success(result);
    }
}