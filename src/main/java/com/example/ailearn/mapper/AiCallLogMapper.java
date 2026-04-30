package com.example.ailearn.mapper;

import com.example.ailearn.model.entity.AiCallLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiCallLogMapper {

    @Insert("""
            INSERT INTO ai_call_log (
                id,
                biz_type,
                model_name,
                user_input,
                prompt,
                response_text,
                final_result,
                success,
                error_message,
                cost_ms,
                need_review,
                gmt_create
            ) VALUES (
                #{id},
                #{bizType},
                #{modelName},
                #{userInput},
                #{prompt},
                #{responseText},
                #{finalResult},
                #{success},
                #{errorMessage},
                #{costMs},
                #{needReview},
                #{gmtCreate}
            )
            """)
    int insert(AiCallLogEntity entity);
}
