package com.example.f_f.chat.service;

import com.example.f_f.chat.entity.ChatMessage;
import com.example.f_f.chat.entity.Conversation;
import com.example.f_f.chat.entity.Role;
import com.example.f_f.chat.repository.ChatMessageRepository;
import com.example.f_f.chat.repository.ConversationRepository;
import com.example.f_f.global.exception.CustomException;
import com.example.f_f.global.exception.RsCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatStoreService {

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;

    @Transactional
    public Conversation saveUserMessage(Long conversationId, String userId, String question) {
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new CustomException(RsCode.CHATROOM_NOT_FOUND));

        if (!conv.getUser().getUserId().equals(userId)) {
            throw new CustomException(RsCode.FORBIDDEN);
        }

        ChatMessage userMsg = ChatMessage.builder()
                .conversation(conv)
                .role(Role.USER)
                .content(question)
                .build();
        messageRepo.save(userMsg);

        return conv;
    }

    @Transactional
    public void saveAssistantMessage(Long conversationId, String answer) {
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new CustomException(RsCode.CHATROOM_NOT_FOUND));

        ChatMessage botMsg = ChatMessage.builder()
                .conversation(conv)
                .role(Role.ASSISTANT)
                .content(answer)
                .build();
        messageRepo.save(botMsg);
    }
}

