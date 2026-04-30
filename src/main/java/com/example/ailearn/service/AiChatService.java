package com.example.ailearn.service;

import com.example.ailearn.config.AiPromptProperties;
import com.example.ailearn.enums.AiBizType;
import com.example.ailearn.model.dao.AiCallLogRecord;
import com.example.ailearn.model.dto.rp.RiskAnalysisResult;
import com.example.ailearn.model.dto.rp.WeeklyReportResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;

    private final RiskAnalysisValidator riskAnalysisValidator;

    private final WeeklyReportValidator weeklyReportValidator;

    private final AiCallLogService aiCallLogService;


    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    private final ObjectMapper objectMapper;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         AiPromptProperties aiPromptProperties,
                         RiskAnalysisValidator riskAnalysisValidator,
                         WeeklyReportValidator weeklyReportValidator,
                         AiCallLogService aiCallLogService,
                         ObjectMapper objectMapper) {
        if (!StringUtils.hasText(aiPromptProperties.getSystem())) {
            throw new IllegalArgumentException("AI System Prompt 未配置，请检查 ai.prompt.system");
        }

        this.chatClient = chatClientBuilder
                .defaultSystem(aiPromptProperties.getSystem())
                .build();
        this.riskAnalysisValidator = riskAnalysisValidator;
        this.weeklyReportValidator = weeklyReportValidator;
        this.aiCallLogService = aiCallLogService;
        this.objectMapper = objectMapper;
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

            String finalAnswer = answer == null ? "AI 未返回有效内容。" : answer;

            log.info("AI普通对话调用成功，耗时：{}ms，用户问题：{}，回答长度：{}",
                    cost,
                    message,
                    finalAnswer.length());

            aiCallLogService.record(buildLogRecord(
                    AiBizType.CHAT.name(),
                    message,
                    message,
                    answer,
                    finalAnswer,
                    true,
                    null,
                    cost,
                    false
            ));

            return finalAnswer;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;

            log.error("AI普通对话调用失败，耗时：{}ms，用户问题：{}", cost, message, e);

            String fallback = "AI 服务暂时不可用，请稍后再试。";

            aiCallLogService.record(buildLogRecord(
                    AiBizType.CHAT.name(),
                    message,
                    message,
                    null,
                    fallback,
                    false,
                    e.getMessage(),
                    cost,
                    true
            ));

            return fallback;
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

            RiskAnalysisResult finalResult = riskAnalysisValidator.validateAndFix(result);

            long cost = System.currentTimeMillis() - start;

            log.info("AI风险分析调用成功，耗时：{}ms，输入数据长度：{}",
                    cost,
                    data.length());

            aiCallLogService.record(buildLogRecord(
                    AiBizType.RISK_ANALYSIS.name(),
                    data,
                    userPrompt,
                    toJsonSafely(result),
                    toJsonSafely(finalResult),
                    true,
                    null,
                    cost,
                    Boolean.FALSE.equals(finalResult.getDataEnough())
            ));

            return finalResult;

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("AI风险分析调用失败，耗时：{}ms，输入数据：{}", cost, data, e);

            RiskAnalysisResult fallback = riskAnalysisValidator.buildFallback("AI 服务调用失败：" + e.getMessage());

            aiCallLogService.record(buildLogRecord(
                    AiBizType.RISK_ANALYSIS.name(),
                    data,
                    userPrompt,
                    null,
                    toJsonSafely(fallback),
                    false,
                    e.getMessage(),
                    cost,
                    true
            ));

            return fallback;
        }
    }

    public WeeklyReportResult generateWeeklyReport(String data) {
        if (!StringUtils.hasText(data)) {
            return weeklyReportValidator.buildFallback("当前未提供周报分析数据");
        }

        String userPrompt = buildWeeklyReportPrompt(data);

        long start = System.currentTimeMillis();

        try {
            WeeklyReportResult result = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(WeeklyReportResult.class);

            WeeklyReportResult finalResult = weeklyReportValidator.validateAndFix(result);

            long cost = System.currentTimeMillis() - start;

            log.info("AI周报生成调用成功，耗时：{}ms，输入数据长度：{}",
                    cost,
                    data.length());

            aiCallLogService.record(buildLogRecord(
                    AiBizType.WEEKLY_REPORT.name(),
                    data,
                    userPrompt,
                    toJsonSafely(result),
                    toJsonSafely(finalResult),
                    true,
                    null,
                    cost,
                    Boolean.FALSE.equals(finalResult.getDataEnough())
            ));

            return finalResult;

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;

            log.error("AI周报生成调用失败，耗时：{}ms，输入数据：{}",
                    cost,
                    data,
                    e);

            WeeklyReportResult fallback = weeklyReportValidator.buildFallback("AI 服务调用失败：" + e.getMessage());

            aiCallLogService.record(buildLogRecord(
                    AiBizType.WEEKLY_REPORT.name(),
                    data,
                    userPrompt,
                    null,
                    toJsonSafely(fallback),
                    false,
                    e.getMessage(),
                    cost,
                    true
            ));

            return fallback;
        }
    }


    private String buildWeeklyReportPrompt(String data) {
        return """
            请根据以下明厨亮灶监管数据生成结构化周报结果。

            输入数据：
            %s

            判断规则：
            1. 如果输入数据缺少接入学校数、预警总数、处置完成率、问题类型等关键指标，则 dataEnough 必须为 false。
            2. 如果输入数据包含接入学校数、食堂数、AI预警总数、主要预警类型、处置完成率、重复预警、设备离线、票证或台账异常等多维数据，则 dataEnough 可以为 true。
            3. 如果 dataEnough 为 false，summary 必须说明“当前数据不足，无法生成完整周报”。
            4. 如果 dataEnough 为 false，suggestions 只能围绕补充数据、核查数据来源、完善统计口径展开，不得直接提出具体业务整改措施。
            5. 如果 dataEnough 为 false，mainProblems 只能描述已提供数据中能直接看出的事实。
            6. 如果 dataEnough 为 true，应围绕平台运行、预警情况、处置闭环、设备在线、票证台账等方面进行总结。
            7. missingFields 仅在数据不足以支撑周报生成时填写；如果已具备周报生成基础，missingFields 返回空数组。

            输出要求：
            1. 返回 WeeklyReportResult 对象；
            2. title 输出固定为“明厨亮灶本周运行周报”；
            3. summary 输出 1 段，总体概括本周运行情况；
            4. keyFindings 输出 3 到 5 条；
            5. mainProblems 输出 3 条；
            6. riskJudgement 输出 1 段风险判断；
            7. suggestions 输出 3 条；
            8. dataEnough 返回 true 或 false。

            限制条件：
            1. 只能基于输入数据进行分析；
            2. 不得编造学校名称、金额、排名、未提供的比例；
            3. 不得把推测原因写成确定事实；
            4. 不得在分析正文中使用“累计”“共计”“本期”“同比”“环比”“全区”“全市”“本周”“本月”“持续上升”等统计口径、范围或趋势表述，除非输入数据明确提供对应口径；固定标题除外。
            5. 不得使用“居前”“排名靠前”“最高”“最低”等排序类表述，除非输入数据明确提供完整排名或对比口径；
            6. 周报语言应正式、简洁，适合教育局管理人员阅读；
            7. 不得输出 Markdown，不得输出代码块。
            8. 如果 dataEnough 为 false，不得主动列举输入数据中未出现的具体预警类型，只能使用“预警类型分布”“预警分类明细”等概括性表述。
            9. 数据不足时，不得使用“实时”“动态”“实时接入”等输入数据未明确提供的数据采集口径。
            10. 如果输入数据未明确说明数据来源或统计状态，不得使用“平台当前记录”“当前系统显示”“实时记录”等表述，应使用“已提供数据中显示”。
            11. 数据不足时，不得要求或生成具体学校名单；如需表达学校维度，只能使用“接入学校数”“学校覆盖范围”“学校分布情况”“学校维度预警分布”等概括性表述，不得使用“各校明细”“各接入学校明细”“各接入学校的预警明细”“学校名单”“高风险区域”“重点单位”“重点监管对象”等较强指向性或结论性表述。            
            12. 不得自行计算占比、比例、增长率、下降率、超过一半、接近一半等数值结论；如需使用比例、占比、环比、同比等结果，必须由输入数据明确提供。
            13. 数字应保持输入中的阿拉伯数字格式，不得将数字、数量、百分比改写为中文数字或中文百分比。
            14. 不得将预警现象直接归因为“意识不足”“责任心不强”“管理松懈”等主观原因，除非输入数据明确提供原因；应优先使用“规范执行不到位”“需进一步加强”等客观表述。
            15. 不得使用“整改流于形式”“管理混乱”“责任缺失”等结论性、问责性表述，除非输入数据明确提供相关依据。
            """
                .formatted(data);
    }


    private AiCallLogRecord buildLogRecord(String bizType,
                                           String userInput,
                                           String prompt,
                                           String responseText,
                                           String finalResult,
                                           Boolean success,
                                           String errorMessage,
                                           Long costMs,
                                           Boolean needReview) {
        AiCallLogRecord record = new AiCallLogRecord();
        record.setBizType(bizType);
        record.setModelName(modelName);
        record.setUserInput(userInput);
        record.setPrompt(prompt);
        record.setResponseText(responseText);
        record.setFinalResult(finalResult);
        record.setSuccess(success);
        record.setErrorMessage(errorMessage);
        record.setCostMs(costMs);
        record.setNeedReview(needReview);
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    private String toJsonSafely(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.warn("对象转JSON失败，objectClass={}", object.getClass().getName(), e);
            return String.valueOf(object);
        }
    }

}
