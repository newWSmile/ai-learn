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

    private final RiskAnalysisValidator riskAnalysisValidator;


    public AiChatService(ChatClient.Builder chatClientBuilder,
                         AiPromptProperties aiPromptProperties,
                         RiskAnalysisValidator riskAnalysisValidator) {
        if (!StringUtils.hasText(aiPromptProperties.getSystem())) {
            throw new IllegalArgumentException("AI System Prompt 未配置，请检查 ai.prompt.system");
        }

        this.chatClient = chatClientBuilder
                .defaultSystem(aiPromptProperties.getSystem())
                .build();
        this.riskAnalysisValidator = riskAnalysisValidator;
    }


    public String chat(String message) {
        if (!StringUtils.hasText(message)) {
            return "请输入需要咨询的问题。";
        }

        long start = System.currentTimeMillis();

        try {
            String answer = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            long cost = System.currentTimeMillis() - start;

            log.info("AI普通对话调用成功，耗时：{}ms，用户问题：{}，回答长度：{}",
                    cost,
                    message,
                    answer == null ? 0 : answer.length());

            return answer == null ? "AI 未返回有效内容。" : answer;

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;

            log.error("AI普通对话调用失败，耗时：{}ms，用户问题：{}", cost, message, e);

            return "AI 服务暂时不可用，请稍后再试。";
        }
    }


    public RiskAnalysisResult analyzeRisk(String data) {
        if (!StringUtils.hasText(data)) {
            return riskAnalysisValidator.buildFallback("当前未提供风险分析数据");
        }

        String userPrompt = """
        请根据以下明厨亮灶监管数据生成结构化风险分析结果。

        输入数据：
        %s

        判断规则：
        1. 如果输入数据只包含单一预警类型，且缺少统计周期、学校范围、处置情况、历史对比等信息，则 dataEnough 必须为 false，riskLevel 必须为 UNKNOWN。
        2. 如果输入数据包含多类预警数量，并同时包含处置完成率、重复预警学校、设备离线等任意一类监管指标，则可视为具备初步风险分析基础，dataEnough 可以为 true。
        3. 如果 dataEnough 为 false，overallJudgement 必须说明“当前数据不足，无法完整判断风险水平”。
        4. 如果 dataEnough 为 false，mainProblems 只能描述已提供数据中能直接看出的事实，不得扩展为确定性的管理原因。
        5. 如果 dataEnough 为 false，suggestions 只能给出补充数据和初步核查建议，不得直接给出处罚、督办或重点监管结论。
        6. 只有在同时具备多维预警数据、处置情况、重复预警或设备状态等数据时，才可以判断 riskLevel 为 LOW、MEDIUM 或 HIGH。
        7. missingFields 仅在数据不足以支撑风险判断时填写；如果已具备初步风险分析基础，missingFields 返回空数组。

        输出要求：
        1. 返回 RiskAnalysisResult 对象；
        2. riskLevel 只能是 LOW、MEDIUM、HIGH、UNKNOWN；
        3. mainProblems 输出 3 条；
        4. suggestions 输出 3 条；
        5. needSupervision 表示是否建议纳入督办关注。

        限制条件：
        1. 只能基于输入数据进行分析；
        2. 不得编造学校名称、金额、排名、未提供的比例；
        3. 不得把推测原因写成确定事实；
        4. 数据不足时必须保守判断；
        5. 不得使用“累计”“共计”“同比”“环比”“全区”“全市”“本周”“本月”“持续上升”等统计口径、范围或趋势表述，除非输入数据明确提供对应口径；可以使用“已提供数据中显示”。
        6. 不得使用“居前”“排名靠前”“最高”“最低”等排序类表述，除非输入数据明确提供完整排名或对比口径。
        7. 数据不足时，整改建议应优先围绕补充统计周期、学校范围、处置状态、原始预警核查、重复触发情况展开，不主动扩展到输入数据未提及的设备、人员、制度问题。
        """.formatted(data);

        long start = System.currentTimeMillis();

        try {
            RiskAnalysisResult result = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(RiskAnalysisResult.class);

            long cost = System.currentTimeMillis() - start;
            log.info("AI风险分析调用成功，耗时：{}ms", cost);

            return riskAnalysisValidator.validateAndFix(result);

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("AI风险分析调用失败，耗时：{}ms，输入数据：{}", cost, data, e);

            return riskAnalysisValidator.buildFallback("AI 服务调用失败：" + e.getMessage());
        }
    }

}
