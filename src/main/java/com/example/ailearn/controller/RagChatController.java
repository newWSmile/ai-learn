package com.example.ailearn.controller;


import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.RagChatResult;
import com.example.ailearn.model.dto.rq.RagChatRequest;
import com.example.ailearn.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/rag")
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/chat")
    public ApiResult<RagChatResult> chat(@RequestBody RagChatRequest request) {
        log.info("收到RAG问答请求, question={}", request == null ? null : request.getQuestion());
        return ApiResult.success(ragChatService.chat(request));
    }
}