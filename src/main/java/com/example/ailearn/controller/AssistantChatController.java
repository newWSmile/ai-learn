package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.AssistantChatResult;
import com.example.ailearn.model.dto.rq.AssistantChatRequest;
import com.example.ailearn.service.AssistantChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 统一AI助手入口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/assistant")
public class AssistantChatController {

    private final AssistantChatService assistantChatService;

    /**
     * 统一AI助手聊天接口
     */
    @PostMapping("/chat")
    public ApiResult<AssistantChatResult> chat(@RequestBody AssistantChatRequest request) {
        AssistantChatResult result = assistantChatService.chat(request);
        return ApiResult.success(result);
    }
}
