package com.example.f_f.chat.dto;


public record QuestionRequest (
    Long conversationId,
    String question
) {}
