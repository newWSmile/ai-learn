package com.example.ailearn.controller;


import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.EmbeddingRetrieveResult;
import com.example.ailearn.model.dto.rq.RagChatRequest;
import com.example.ailearn.service.EmbeddingRetrieveTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/rag/embedding")
public class EmbeddingRetrieveTestController {

    private final EmbeddingRetrieveTestService embeddingRetrieveTestService;

    @PostMapping("/retrieve")
    public ApiResult<EmbeddingRetrieveResult> retrieve(@RequestBody RagChatRequest request) {
        log.info("收到Embedding知识检索测试请求, question={}", request == null ? null : request.getQuestion());
        return ApiResult.success(embeddingRetrieveTestService.retrieve(request));
    }
}