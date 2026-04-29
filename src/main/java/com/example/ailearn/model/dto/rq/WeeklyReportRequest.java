package com.example.ailearn.model.dto.rq;

public class WeeklyReportRequest {

    /**
     * 周报分析数据
     */
    private String data;

    public WeeklyReportRequest() {
    }

    public WeeklyReportRequest(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
