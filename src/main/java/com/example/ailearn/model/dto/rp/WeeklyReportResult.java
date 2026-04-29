package com.example.ailearn.model.dto.rp;

import java.util.List;

public class WeeklyReportResult {

    /**
     * 周报标题
     */
    private String title;

    /**
     * 总体摘要
     */
    private String summary;

    /**
     * 关键发现
     */
    private List<String> keyFindings;

    /**
     * 主要问题
     */
    private List<String> mainProblems;

    /**
     * 风险判断
     */
    private String riskJudgement;

    /**
     * 工作建议
     */
    private List<String> suggestions;

    /**
     * 数据是否充足
     */
    private Boolean dataEnough;

    /**
     * 缺失字段
     */
    private List<String> missingFields;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeyFindings() {
        return keyFindings;
    }

    public void setKeyFindings(List<String> keyFindings) {
        this.keyFindings = keyFindings;
    }

    public List<String> getMainProblems() {
        return mainProblems;
    }

    public void setMainProblems(List<String> mainProblems) {
        this.mainProblems = mainProblems;
    }

    public String getRiskJudgement() {
        return riskJudgement;
    }

    public void setRiskJudgement(String riskJudgement) {
        this.riskJudgement = riskJudgement;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Boolean getDataEnough() {
        return dataEnough;
    }

    public void setDataEnough(Boolean dataEnough) {
        this.dataEnough = dataEnough;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

}
