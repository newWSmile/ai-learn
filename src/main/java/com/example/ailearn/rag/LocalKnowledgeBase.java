package com.example.ailearn.rag;


import com.example.ailearn.enums.KnowledgeCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalKnowledgeBase {

    private final List<KnowledgeChunk> chunks = List.of(
            KnowledgeChunk.builder()
                    .id("mclz_alarm_type")
                    .title("明厨亮灶AI预警类型")
                    .category(KnowledgeCategory.ALARM_TYPE)
                    .source("系统内置知识库")
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
                    .keywords(List.of(
                            "预警类型",
                            "AI预警类型",
                            "支持哪些预警",
                            "有哪些预警",
                            "预警清单",
                            "明厨亮灶预警"
                    ))
                    .priority(100)
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_camera_offline")
                    .title("摄像头离线说明")
                    .category(KnowledgeCategory.DEVICE_OPERATION)
                    .source("设备运维规则")
                    .content("""
                            摄像头离线是指明厨亮灶视频监控点位无法正常接入平台，
                            可能影响后厨视频查看、AI识别、预警留痕和监管取证。
                            在监管报告中，摄像头离线应作为设备运维类问题描述。
                            如输入数据未提供具体原因，不应直接归因为学校管理不到位。
                            """)
                    .keywords(List.of(
                            "摄像头离线",
                            "视频点位掉线",
                            "点位掉线",
                            "掉线",
                            "无法接入",
                            "不在线",
                            "断开",
                            "视频无法查看",
                            "监控无法接入"
                    ))
                    .priority(90)
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_camera_blocked")
                    .title("摄像头遮挡说明")
                    .category(KnowledgeCategory.DEVICE_OPERATION)
                    .source("设备运维规则")
                    .content("""
                            摄像头遮挡是指视频画面被物体、人员或其他因素遮挡，
                            导致后厨关键区域无法被正常查看或识别。
                            在监管报告中，可表述为“部分视频点位存在画面遮挡情况，
                            需进一步核查点位环境并恢复有效监看”。
                            不宜在缺少依据时直接认定为人为故意遮挡。
                            """)
                    .keywords(List.of(
                            "摄像头遮挡",
                            "画面遮挡",
                            "监控画面被挡住",
                            "画面被挡住",
                            "被挡住",
                            "挡住",
                            "遮挡",
                            "看不见",
                            "无法查看",
                            "关键区域无法查看",
                            "视频画面异常"
                    ))
                    .priority(80)
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_garbage_bin")
                    .title("垃圾桶未盖预警说明")
                    .category(KnowledgeCategory.ALARM_EXPLANATION)
                    .source("AI预警解释规则")
                    .content("""
                            垃圾桶未盖属于后厨环境卫生类AI预警。
                            该类预警通常用于提示废弃物存放、后厨卫生管理、操作区域环境规范等问题。
                            在正式分析中，可以表述为“环境卫生相关操作需进一步规范”。
                            不宜直接使用“管理混乱”“责任缺失”等问责性表述。
                            """)
                    .keywords(List.of("垃圾桶未盖", "环境卫生", "废弃物", "卫生管理", "后厨环境"))
                    .priority(90)
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_report_style")
                    .title("明厨亮灶监管报告表达要求")
                    .category(KnowledgeCategory.REPORT_STYLE)
                    .source("报告表达规则")
                    .content("""
                            明厨亮灶监管报告应保持正式、客观、审慎。
                            不得编造学校名称、金额、排名、比例。
                            不得将预警现象直接归因为“意识不足”“责任心不强”“管理松懈”等主观原因。
                            数据不足时，应说明“当前数据不足，无法判断”。
                            建议使用“已提供数据中显示”“需进一步核查”“建议补充统计口径”等保守表达。
                            """)
                    .keywords(List.of(
                            "监管报告",
                            "报告表达",
                            "报告怎么写",
                            "怎么写",
                            "如何写",
                            "怎么描述",
                            "如何描述",
                            "描述",
                            "表述",
                            "分析",
                            "报告口径",
                            "保守表达"
                    ))
                    .priority(95)
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_weekly_report_data_enough")
                    .title("周报数据完整性规则")
                    .category(KnowledgeCategory.REPORT_STYLE)
                    .source("周报生成规则")
                    .content("""
                            生成明厨亮灶周报时，建议至少具备接入学校数、食堂数、AI预警总数、
                            主要预警类型、处置完成率、重复预警、设备离线、票证或台账异常等多维数据。
                            如果仅提供单一指标，例如只有AI预警总数，应判断为数据不足，
                            不宜生成完整周报，只能提示补充数据或完善统计口径。
                            """)
                    .keywords(List.of("周报", "数据完整", "dataEnough", "预警总数", "处置完成率", "重复预警", "设备离线"))
                    .priority(100)
                    .build(),

            KnowledgeChunk.builder()
                    .id("mclz_morning_check_not_submitted")
                    .title("晨检台账未提交说明")
                    .category(KnowledgeCategory.LEDGER_RULE)
                    .source("台账管理规则")
                    .content("""
                            晨检台账未提交是指学校或食堂未按要求记录或提交从业人员晨检情况。
                            该类问题属于台账管理类问题，通常用于提示学校在从业人员健康检查、晨检记录留痕、台账提交及时性等方面需要进一步规范。
                            在监管报告中，可表述为"晨检台账存在未提交情况，需进一步核查从业人员晨检记录及台账补录情况"。
                            如输入数据未提供具体原因，不应直接归因为学校管理不到位或人员责任缺失。
                            """)
                    .keywords(List.of(
                            "晨检",
                            "晨检台账",
                            "晨午检",
                            "台账未提交",
                            "未提交",
                            "没有提交",
                            "未填报",
                            "未上报",
                            "从业人员晨检",
                            "健康检查",
                            "台账补录"
                    ))
                    .priority(85)
                    .build()
    );

    public List<KnowledgeChunk> listAll() {
        return chunks;
    }
}