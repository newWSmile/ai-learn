package com.example.ailearn.controller;


import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.EmbeddingTestResult;
import com.example.ailearn.model.dto.rq.EmbeddingTestRequest;
import com.example.ailearn.service.EmbeddingTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/embedding")
public class EmbeddingTestController {

    private final EmbeddingTestService embeddingTestService;

    @PostMapping("/test")
    public ApiResult<EmbeddingTestResult> test(@RequestBody EmbeddingTestRequest request) {
        log.info("收到Embedding测试请求, text={}", request == null ? null : request.getText());
        return ApiResult.success(embeddingTestService.embed(request));
    }
}