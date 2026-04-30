package com.example.ailearn.rag;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

/**
 * 待补充知识处理建议生成器
 *
 * 说明：
 * 这里只做规则判断，不调用大模型。
 * 目的是给运营或产品人员一个初步处理方向。
 */
@Component
public class MissingKnowledgeActionSuggester {

    /**
     * 根据用户问题生成建议动作
     */
    public String suggest(String question) {
        if (StrUtil.isBlank(question)) {
            return "问题内容为空，建议检查日志记录是否完整。";
        }

        if (containsAny(question, "合同", "采购合同", "协议")) {
            return "建议确认合同生成是否属于当前产品能力范围；如果属于，请补充采购合同相关产品文档、接口说明或业务规则。";
        }

        if (containsAny(question, "台账", "晨检", "留样", "消毒", "陪餐")) {
            return "建议补充台账类业务规则，包括适用场景、统计口径、异常判断和报告表达方式。";
        }

        if (containsAny(question, "摄像头", "监控", "视频", "点位", "离线", "遮挡")) {
            return "建议补充设备运维类知识，包括问题定义、影响范围、报告表述和通用处置建议。";
        }

        if (containsAny(question, "预警", "AI", "算法", "识别")) {
            return "建议补充 AI 预警类知识，包括预警类型、业务含义、处置口径和报告表达要求。";
        }

        if (containsAny(question, "报表", "周报", "月报", "日报", "报告")) {
            return "建议补充报告生成类知识，包括数据口径、字段含义、生成规则和禁止编造要求。";
        }

        return "建议确认该问题是否属于当前产品能力范围；如果属于，请补充对应产品文档、接口说明或业务规则。";
    }


    /**
     * 根据问题生成建议分类
     */
    public String suggestCategory(String question) {
        if (StrUtil.isBlank(question)) {
            return "未知";
        }

        if (containsAny(question, "合同", "采购合同", "协议")) {
            return "合同类";
        }

        if (containsAny(question, "台账", "晨检", "晨午检", "留样", "消毒", "陪餐")) {
            return "台账类";
        }

        if (containsAny(question, "摄像头", "监控", "视频", "点位", "离线", "遮挡")) {
            return "设备运维类";
        }

        if (containsAny(question, "预警", "AI", "算法", "识别")) {
            return "AI预警类";
        }

        if (containsAny(question, "报表", "周报", "月报", "日报", "报告")) {
            return "报告生成类";
        }

        return "其他";
    }


    /**
     * 判断文本是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }

        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
