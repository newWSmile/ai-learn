package com.example.ailearn.service;

import com.example.ailearn.config.AiPromptProperties;
import com.example.ailearn.model.dto.rp.RiskAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;


    public AiChatService(ChatClient.Builder chatClientBuilder,
                         AiPromptProperties aiPromptProperties) {
        if (!StringUtils.hasText(aiPromptProperties.getSystem())) {
            throw new IllegalArgumentException("AI System Prompt 未配置，请检查 ai.prompt.system");
        }

        this.chatClient = chatClientBuilder
                .defaultSystem(aiPromptProperties.getSystem())
                .build();
    }

    long start = System.currentTimeMillis();

    public String chat(String message) {
        if (!StringUtils.hasText(message)) {
            return "请输入需要咨询的问题。";
        }


        try{
            String answer = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();


            log.info("AI回答长度: {}", answer == null ? 0 : answer.length());
            return answer == null ? "AI 未返回有效内容。" : answer;
        }catch (Exception e){
            log.info("AI服务调用异常: {}", e.getMessage());
        }
        log.info("用户问题: {}", message);

        long cost = System.currentTimeMillis() - start;
        log.info("ai 调用耗时: {} ms", cost);

        return "AI 服务暂时不可用，请稍后再试。";


    }


    public RiskAnalysisResult analyzeRisk(String data) {
        if (!StringUtils.hasText(data)) {
            RiskAnalysisResult result = new RiskAnalysisResult();
            result.setOverallJudgement("当前数据不足，无法判断");
            result.setRiskLevel("UNKNOWN");
            result.setMainProblems(List.of());
            result.setRiskAnalysis("当前数据不足，无法判断");
            result.setSuggestions(List.of());
            result.setNeedSupervision(false);
            result.setDataEnough(false);
            result.setMissingFields(List.of("data"));
            return result;
        }

        String userPrompt = """
        请根据以下明厨亮灶监管数据生成结构化风险分析结果。

        输入数据：
        %s

        判断规则：
        1. 如果输入数据只包含单一预警类型，且缺少统计周期、学校范围、处置情况、历史对比等信息，则 dataEnough 必须为 false，riskLevel 必须为 UNKNOWN。
        2. 如果 dataEnough 为 false，overallJudgement 必须说明“当前数据不足，无法完整判断风险水平”。
        3. 如果 dataEnough 为 false，mainProblems 只能描述已提供数据中能直接看出的事实，不得扩展为确定性的管理原因。
        4. 如果 dataEnough 为 false，suggestions 只能给出补充数据和初步核查建议，不得直接给出处罚、督办或重点监管结论。
        5. 只有在同时具备预警类型、预警数量、处置情况、重复预警或设备状态等多维数据时，才可以判断 riskLevel 为 LOW、MEDIUM 或 HIGH。

        输出要求：
        1. 返回 RiskAnalysisResult 对象；
        2. riskLevel 只能是 LOW、MEDIUM、HIGH、UNKNOWN；
        3. mainProblems 输出 3 条；
        4. suggestions 输出 3 条；
        5. missingFields 必须列出当前分析缺少的关键字段；
        6. needSupervision 表示是否建议纳入督办关注。

        限制条件：
        1. 只能基于输入数据进行分析；
        2. 不得编造学校名称、金额、排名、未提供的比例；
        3. 不得把推测原因写成确定事实；
        4. 数据不足时必须保守判断。
        5. 不得使用“累计”“同比”“环比”“全区”“全市”“持续上升”等输入数据未明确提供的统计口径或趋势表述。
        """.formatted(data);

        long start = System.currentTimeMillis();

        try {
            RiskAnalysisResult result = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(RiskAnalysisResult.class);

            long cost = System.currentTimeMillis() - start;
            log.info("AI风险分析调用成功，耗时：{}ms", cost);

            return result;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("AI风险分析调用失败，耗时：{}ms", cost);
            log.error("异常信息：{}", e.getMessage());

            RiskAnalysisResult fallback = new RiskAnalysisResult();
            fallback.setOverallJudgement("AI 服务暂时不可用，无法完成风险分析");
            fallback.setRiskLevel("UNKNOWN");
            fallback.setMainProblems(List.of());
            fallback.setRiskAnalysis("AI 服务调用失败：" + e.getMessage());
            fallback.setSuggestions(List.of());
            fallback.setNeedSupervision(false);
            fallback.setDataEnough(false);
            fallback.setMissingFields(List.of());

            return fallback;
        }
    }

}
