package com.example.f_f.chat.controller;

import com.example.f_f.chat.dto.AnswerResponse;
import com.example.f_f.chat.dto.ChatMessageDto;
import com.example.f_f.chat.dto.QuestionRequest;
import com.example.f_f.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/messages")
    public ResponseEntity<Page<ChatMessageDto>> listMessages(Authentication auth,
                                                             @RequestParam Long conversationId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "5") int size) {

        return ResponseEntity.ok(chatMessageService.listMessages(auth.getName(), conversationId, page, size));
    }

    @PostMapping("/ask")
    public Mono<ResponseEntity<AnswerResponse>> ask(Authentication auth,
                                                    @RequestBody QuestionRequest req) {

        return chatMessageService
                .addAssistantMessage(req.conversationId(), auth.getName(), req.question())
                .map(ResponseEntity::ok);
    }
}
