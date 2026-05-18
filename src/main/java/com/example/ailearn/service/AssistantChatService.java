package com.example.ailearn.service;

import cn.hutool.core.util.StrUtil;
import com.example.ailearn.enums.AssistantRouteType;
import com.example.ailearn.model.dto.rp.AiToolChatResult;
import com.example.ailearn.model.dto.rp.AssistantChatResult;
import com.example.ailearn.model.dto.rp.AssistantRouteDecision;
import com.example.ailearn.model.dto.rp.RagChatResult;
import com.example.ailearn.model.dto.rq.AiToolChatRequest;
import com.example.ailearn.model.dto.rq.AssistantChatRequest;
import com.example.ailearn.model.dto.rq.RagChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 统一AI助手服务
 *
 * 作用：
 * 对外提供一个统一入口，根据问题自动分发到普通聊天、RAG问答或工具调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantChatService {

    private final AssistantRouterService assistantRouterService;

    private final RagChatService ragChatService;

    private final AiToolCallingService aiToolCallingService;

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 统一AI助手聊天入口
     */
    public AssistantChatResult chat(AssistantChatRequest request) {
        String question = request == null ? null : request.getQuestion();

        if (StrUtil.isBlank(question)) {
            AssistantChatResult result = new AssistantChatResult();
            result.setAnswer("问题不能为空，请输入需要咨询的内容。");
            result.setRouteType(AssistantRouteType.NORMAL_CHAT.name());
            result.setKnowledgeHit(false);
            result.setToolUsed(false);
            result.setNeedReview(true);
            return result;
        }

        long startTime = System.currentTimeMillis();

        AssistantRouteDecision decision = assistantRouterService.decide(question);

        log.info("AI助手路由完成，question={}，routeType={}，reason={}",
                question, decision.getRouteType(), decision.getReason());

        AssistantRouteType routeType = AssistantRouteType.valueOf(decision.getRouteType());

        AssistantChatResult result;

        switch (routeType) {
            case RAG_CHAT:
                result = handleRagChat(question);
                break;

            case TOOL_CALL:
                result = handleToolCall(question);
                break;

            case NORMAL_CHAT:
            default:
                result = handleNormalChat(question);
                break;
        }

        log.info("AI助手处理完成，question={}，routeType={}，costMs={}",
                question, result.getRouteType(), System.currentTimeMillis() - startTime);

        return result;
    }

    /**
     * 处理RAG知识库问答
     */
    private AssistantChatResult handleRagChat(String question) {
        RagChatRequest ragRequest = new RagChatRequest();
        ragRequest.setQuestion(question);

        RagChatResult ragResult = ragChatService.chat(ragRequest);

        AssistantChatResult result = new AssistantChatResult();
        result.setAnswer(ragResult.getAnswer());
        result.setRouteType(AssistantRouteType.RAG_CHAT.name());
        result.setKnowledgeHit(ragResult.getKnowledgeHit());
        result.setToolUsed(false);
        result.setToolName(null);
        result.setRawResult(ragResult);
        result.setNeedReview(ragResult.getNeedReview());

        return result;
    }

    /**
     * 处理工具调用
     */
    private AssistantChatResult handleToolCall(String question) {
        AiToolChatRequest toolRequest = new AiToolChatRequest();
        toolRequest.setQuestion(question);

        AiToolChatResult toolResult = aiToolCallingService.chat(toolRequest);

        AssistantChatResult result = new AssistantChatResult();
        result.setAnswer(toolResult.getAnswer());
        result.setRouteType(AssistantRouteType.TOOL_CALL.name());
        result.setKnowledgeHit(false);
        result.setToolUsed(toolResult.getToolUsed());
        result.setToolName(toolResult.getToolName());
        result.setRawResult(toolResult);
        result.setNeedReview(toolResult.getNeedReview());

        return result;
    }

    /**
     * 处理普通聊天
     */
    private AssistantChatResult handleNormalChat(String question) {
        String answer;

        try {
            answer = chatClientBuilder.build()
                    .prompt()
                    .user(buildNormalChatPrompt(question))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("普通聊天调用失败，question={}", question, e);
            answer = "当前普通对话处理失败，请稍后重试。";
        }

        AssistantChatResult result = new AssistantChatResult();
        result.setAnswer(answer);
        result.setRouteType(AssistantRouteType.NORMAL_CHAT.name());
        result.setKnowledgeHit(false);
        result.setToolUsed(false);
        result.setToolName(null);
        result.setRawResult(null);
        result.setNeedReview(false);

        return result;
    }

    /**
     * 普通聊天Prompt
     */
    private String buildNormalChatPrompt(String question) {
        return """
                你是一个简洁、友好的AI助手。
                请直接回答用户问题。
                如果用户只是问候，请简短回应。
                
                用户问题：
                %s
                """.formatted(question);
    }
}