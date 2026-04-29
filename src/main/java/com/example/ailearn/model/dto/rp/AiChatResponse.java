package com.example.ailearn.model.dto.rp;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiChatResponse {

    private String answer;

    public AiChatResponse(String answer) {
        this.answer = answer;
    }

}
