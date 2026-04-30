package com.example.ailearn.enums;

public enum AiBizType {

    CHAT("普通对话"),
    RISK_ANALYSIS("风险分析"),
    WEEKLY_REPORT("周报生成");

    private final String desc;

    AiBizType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
