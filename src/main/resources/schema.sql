CREATE TABLE IF NOT EXISTS ai_call_log (
                                           id TEXT PRIMARY KEY,
                                           biz_type TEXT NOT NULL,
                                           model_name TEXT,
                                           user_input TEXT,
                                           prompt TEXT,
                                           response_text TEXT,
                                           final_result TEXT,
                                           success INTEGER NOT NULL DEFAULT 1,
                                           error_message TEXT,
                                           cost_ms INTEGER,
                                           need_review INTEGER NOT NULL DEFAULT 0,
                                           gmt_create TEXT
);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_biz_type
    ON ai_call_log (biz_type);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_success
    ON ai_call_log (success);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_need_review
    ON ai_call_log (need_review);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_gmt_create
    ON ai_call_log (gmt_create);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_biz_success_time
    ON ai_call_log (biz_type, success, gmt_create);


-- RAG知识片段表
-- 用途：存储可被RAG检索和拼接进Prompt的知识片段
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    -- 知识片段ID，建议使用业务可读ID，例如 mclz_camera_offline
                                               id VARCHAR(64) PRIMARY KEY,

    -- 知识标题，例如 摄像头离线说明、晨检台账未提交说明
                                               title VARCHAR(200) NOT NULL,

    -- 知识分类
    -- 示例：
    -- ALARM_TYPE：预警类型
    -- ALARM_EXPLANATION：预警解释
    -- REPORT_STYLE：报告表达
    -- DEVICE_OPERATION：设备运维
    -- LEDGER_RULE：台账规则
                                               category VARCHAR(50) NOT NULL,

    -- 知识来源，例如 系统内置知识库、设备运维规则、台账管理规则、报告表达规则
                                               source VARCHAR(200),

    -- 知识正文，用于拼接到RAG Prompt中作为模型回答依据
                                               content TEXT NOT NULL,

    -- 关键词，学习阶段使用英文逗号分隔
    -- 示例：摄像头离线,点位掉线,无法接入
    -- 后续可改为JSON数组
                                               keywords TEXT,

    -- 优先级，数字越大越优先，用于同等命中情况下排序
                                               priority INTEGER DEFAULT 0,

    -- 是否启用：1启用，0禁用；禁用后不参与RAG检索
                                               enabled INTEGER DEFAULT 1,

    -- 创建时间，格式 yyyy-MM-dd HH:mm:ss
                                               gmt_create VARCHAR(20),

    -- 修改时间，格式 yyyy-MM-dd HH:mm:ss
                                               gmt_modified VARCHAR(20)
);

-- 知识分类索引：便于后续按分类筛选知识
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_category
    ON knowledge_chunk(category);

-- 启用状态 + 优先级索引：便于加载启用知识并按优先级排序
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_enabled_priority
    ON knowledge_chunk(enabled, priority);

-- 修改时间索引：便于后续查询最近修改的知识片段
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_gmt_modified
    ON knowledge_chunk(gmt_modified);



-- RAG知识片段Embedding向量表
-- 用途：保存 knowledge_chunk 对应的 Embedding 向量，避免每次启动或reload都重新调用Embedding模型
CREATE TABLE IF NOT EXISTS knowledge_chunk_embedding (
    -- 主键ID，建议使用雪花ID
                                                         id VARCHAR(64) PRIMARY KEY,

    -- 知识片段ID，对应 knowledge_chunk.id
                                                         knowledge_id VARCHAR(64) NOT NULL,

    -- Embedding模型名称，例如 text-embedding-v4
                                                         model_name VARCHAR(100) NOT NULL,

    -- 向量维度，例如 1024
                                                         dimension INTEGER,

    -- 知识向量化文本的哈希值
    -- 用途：判断知识标题、内容、关键词等是否发生变化
                                                         content_hash VARCHAR(128) NOT NULL,

    -- Embedding向量数据
    -- 学习阶段使用JSON数组字符串存储，例如 [0.001, -0.002, ...]
                                                         embedding_vector TEXT NOT NULL,

    -- 创建时间，格式 yyyy-MM-dd HH:mm:ss
                                                         gmt_create VARCHAR(20),

    -- 修改时间，格式 yyyy-MM-dd HH:mm:ss
                                                         gmt_modified VARCHAR(20)
);

-- 同一个知识片段，在同一个模型下只能有一条向量记录
CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_chunk_embedding_knowledge_model
    ON knowledge_chunk_embedding(knowledge_id, model_name);

-- 模型名称索引：便于后续按模型查询或迁移
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_model_name
    ON knowledge_chunk_embedding(model_name);

-- 内容哈希索引：便于判断知识内容是否变化
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_content_hash
    ON knowledge_chunk_embedding(content_hash);

-- 修改时间索引：便于后续排查最近更新的向量
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_gmt_modified
    ON knowledge_chunk_embedding(gmt_modified);