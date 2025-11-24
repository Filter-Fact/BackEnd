package com.example.f_f.chat.service;

import com.example.f_f.chat.dto.AnswerResponse;
import com.example.f_f.chat.dto.UserQuestionDto;
import com.example.f_f.chat.entity.Role;
import com.example.f_f.chat.dto.ChatMessageDto;
import com.example.f_f.chat.entity.ChatMessage;
import com.example.f_f.chat.entity.Conversation;
import com.example.f_f.chat.repository.ChatMessageRepository;
import com.example.f_f.chat.repository.ConversationRepository;
import com.example.f_f.global.exception.CustomException;
import com.example.f_f.global.exception.RsCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;

    private final WebClient fastApiClient;

    @Value("${fastapi.ask-path}")
    private String askPath;

    @Transactional
    public AnswerResponse addAssistantMessage(Long conversationId, String userId, String userQuestion) {

        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new CustomException(RsCode.CHATROOM_NOT_FOUND));

        if (!conv.getUser().getUserId().equals(userId)) {
            throw new CustomException(RsCode.FORBIDDEN);
        }

        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conv)
                .role(Role.USER)
                .content(userQuestion)
                .build();
        messageRepo.save(userMsg);

         AnswerResponse aiAnswer = fastApiClient.post()
                .uri(askPath)
                .bodyValue(new UserQuestionDto(userQuestion))
                .retrieve()
                .bodyToMono(AnswerResponse.class)
                .block(Duration.ofSeconds(3000));

        if (aiAnswer == null || aiAnswer.answer() == null || aiAnswer.answer().isBlank()) {
            throw new CustomException(RsCode.AI_EMPTY_RESPONSE);
        }

        ChatMessage botMsg = ChatMessage.builder()
                .conversation(conv)
                .role(Role.ASSISTANT)
                .content(aiAnswer.answer())
                .build();
        messageRepo.save(botMsg);

        return aiAnswer;
    }


    public Page<ChatMessageDto> listMessages(String userId, Long conversationId, int page, int size) {
        Conversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new CustomException(RsCode.CHATROOM_NOT_FOUND));

        if (!conversation.getUser().getUserId().equals(userId)) {
            throw new CustomException(RsCode.FORBIDDEN);
        }

        return messageRepo.findMessageDtosByConversationId(
                conversationId, PageRequest.of(page, size));
    }
}
