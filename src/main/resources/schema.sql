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