package com.example.f_f.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record UserQuestionDto(
        @NotBlank String question
) {}