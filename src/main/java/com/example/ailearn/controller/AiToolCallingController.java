package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.AiToolChatResult;
import com.example.ailearn.model.dto.rq.AiToolChatRequest;
import com.example.ailearn.service.AiToolCallingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 工具调用测试接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/tool")
public class AiToolCallingController {

    private final AiToolCallingService aiToolCallingService;

    /**
     * AI 工具调用聊天接口
     */
    @PostMapping("/chat")
    public ApiResult<AiToolChatResult> chat(@RequestBody AiToolChatRequest request) {
        AiToolChatResult result = aiToolCallingService.chat(request);
        return ApiResult.success(result);
    }
}