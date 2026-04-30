package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.HybridRetrieveResult;
import com.example.ailearn.model.dto.rq.HybridRetrieveRequest;
import com.example.ailearn.service.HybridRetrieveTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 混合检索测试接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/rag/hybrid")
public class HybridRetrieveTestController {

    private final HybridRetrieveTestService hybridRetrieveTestService;

    /**
     * 混合检索测试接口
     *
     * 请求地址：
     * POST /ai/rag/hybrid/retrieve
     */
    @PostMapping("/retrieve")
    public ApiResult<HybridRetrieveResult> retrieve(@RequestBody HybridRetrieveRequest request) {
        HybridRetrieveResult result = hybridRetrieveTestService.retrieve(request);
        return ApiResult.success(result);
    }
}
