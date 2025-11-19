package com.example.f_f.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QuestionRequest {
    private Long conversationId;
    private String question;
}
