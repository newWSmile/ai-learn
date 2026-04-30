package com.example.ailearn.controller;


import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rq.AiCallLogQueryRequest;
import com.example.ailearn.model.vo.AiCallLogDetailVO;
import com.example.ailearn.model.vo.AiCallLogListVO;
import com.example.ailearn.model.vo.AiCallLogVO;
import com.example.ailearn.service.AiCallLogQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/log")
public class AiCallLogController {

    private final AiCallLogQueryService aiCallLogQueryService;

    @GetMapping("/recent")
    public ApiResult<List<AiCallLogListVO>> recent(@ModelAttribute AiCallLogQueryRequest request) {
        log.info("收到AI调用日志列表查询请求, request={}", request);
        return ApiResult.success(aiCallLogQueryService.recent(request));
    }

    @GetMapping("/detail")
    public ApiResult<AiCallLogDetailVO> detail(@RequestParam String id) {
        log.info("收到AI调用日志详情查询请求, id={}", id);
        AiCallLogDetailVO detail = aiCallLogQueryService.detail(id);
        return ApiResult.success(detail);
    }
}