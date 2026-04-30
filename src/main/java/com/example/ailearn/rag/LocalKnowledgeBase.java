package com.example.ailearn.rag;


import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalKnowledgeBase {

    private final List<KnowledgeChunk> chunks = List.of(
            KnowledgeChunk.builder()
                    .id("mclz_alarm_type")
                    .title("明厨亮灶AI预警类型")
                    .content("""
                            当前平台支持的明厨亮灶AI预警类型包括：
                            1. 未戴帽子；
                            2. 未戴口罩；
                            3. 未着工装；
                            4. 抽烟；
                            5. 鼠患；
                            6. 火情；
                            7. 垃圾桶未盖；
                            8. 摄像头遮挡；
                            9. 摄像头离线；
                            10. 视频时间异常。
                            """)
                    .keywords(List.of("预警类型", "AI预警", "明厨亮灶", "未戴帽子", "未戴口罩", "摄像头离线"))
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_offline_camera")
                    .title("摄像头离线说明")
                    .content("""
                            摄像头离线是指明厨亮灶视频监控点位无法正常接入平台，
                            可能影响后厨视频查看、AI识别、预警留痕和监管取证。
                            在监管报告中，摄像头离线应作为设备运维类问题进行描述，
                            不应直接归因为学校管理不到位，除非输入数据明确提供原因。
                            """)
                    .keywords(List.of("摄像头离线", "设备离线", "视频离线", "设备运维"))
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_garbage_bin")
                    .title("垃圾桶未盖预警说明")
                    .content("""
                            垃圾桶未盖属于后厨环境卫生类AI预警。
                            该类预警通常用于提示废弃物存放、后厨卫生管理、操作区域环境规范等问题。
                            在正式分析中，可以表述为“环境卫生相关操作需进一步规范”，
                            不宜直接使用“管理混乱”“责任缺失”等问责性表述。
                            """)
                    .keywords(List.of("垃圾桶未盖", "环境卫生", "废弃物", "卫生管理"))
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_report_style")
                    .title("明厨亮灶监管报告表达要求")
                    .content("""
                            明厨亮灶监管报告应保持正式、客观、审慎。
                            不得编造学校名称、金额、排名、比例。
                            不得将预警现象直接归因为“意识不足”“责任心不强”“管理松懈”等主观原因。
                            数据不足时，应说明“当前数据不足，无法判断”。
                            建议使用“已提供数据中显示”“需进一步核查”“建议补充统计口径”等保守表达。
                            """)
                    .keywords(List.of("报告", "周报", "监管报告", "表达要求", "数据不足", "不得编造"))
                    .build()
    );

    public List<KnowledgeChunk> listAll() {
        return chunks;
    }
}