package com.example.ailearn.controller;

import com.example.ailearn.model.dto.base.ApiResult;
import com.example.ailearn.model.dto.rp.AiChatResponse;
import com.example.ailearn.model.dto.rp.RiskAnalysisResult;
import com.example.ailearn.model.dto.rp.WeeklyReportResult;
import com.example.ailearn.model.dto.rq.AiChatRequest;
import com.example.ailearn.model.dto.rq.RiskAnalysisRequest;
import com.example.ailearn.model.dto.rq.WeeklyReportRequest;
import com.example.ailearn.service.AiChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }


    @PostMapping("/chat")
    public ApiResult<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        String answer = aiChatService.chat(request.getMessage());
        return ApiResult.success(new AiChatResponse(answer));
    }


    @PostMapping("/risk/analyze")
    public ApiResult<RiskAnalysisResult> analyzeRisk(@RequestBody RiskAnalysisRequest request) {
        RiskAnalysisResult result = aiChatService.analyzeRisk(request.getData());
        return ApiResult.success(result);
    }


    @PostMapping("/report/weekly")
    public ApiResult<WeeklyReportResult> generateWeeklyReport(@RequestBody WeeklyReportRequest request) {
        WeeklyReportResult result = aiChatService.generateWeeklyReport(request.getData());
        return ApiResult.success(result);
    }


}
