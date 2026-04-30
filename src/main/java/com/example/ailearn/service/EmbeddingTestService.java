package com.example.ailearn.service;


import com.example.ailearn.model.dto.rp.EmbeddingTestResult;
import com.example.ailearn.model.dto.rq.EmbeddingTestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingTestService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingTestResult embed(EmbeddingTestRequest request) {
        String text = request == null ? null : request.getText();

        if (text == null || text.isBlank()) {
            return EmbeddingTestResult.builder()
                    .dimension(0)
                    .preview("")
                    .success(false)
                    .build();
        }

        float[] vector = embeddingModel.embed(text);

        log.info("文本向量生成成功, text={}, dimension={}", text, vector.length);

        return EmbeddingTestResult.builder()
                .dimension(vector.length)
                .preview(Arrays.toString(Arrays.copyOf(vector, Math.min(5, vector.length))))
                .success(true)
                .build();
    }
}