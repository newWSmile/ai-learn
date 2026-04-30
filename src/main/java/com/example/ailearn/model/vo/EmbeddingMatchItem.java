package com.example.ailearn.model.vo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingMatchItem {

    private String title;

    private String source;

    private Double score;
}