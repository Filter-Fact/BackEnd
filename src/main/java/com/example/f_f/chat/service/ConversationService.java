package com.example.f_f.chat.service;

import com.example.f_f.chat.dto.StartConversationResponse;
import com.example.f_f.chat.entity.Conversation;
import com.example.f_f.chat.repository.ConversationRepository;
import com.example.f_f.global.exception.CustomException;
import com.example.f_f.global.exception.RsCode;
import com.example.f_f.user.entity.User;
import com.example.f_f.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Conversation startConversation(String userId, String title) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(RsCode.USER_NOT_FOUND));

        Conversation c = Conversation.builder()
                .user(user)
                .title((title == null || title.isBlank()) ? "새 대화" : title)
                .build();
        return conversationRepository.save(c);
    }

    public Page<StartConversationResponse> listConversations(String userId, int page, int size) {
        return conversationRepository.findByUser_UserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
    }

    public Conversation getOwnedConversation(String userId, Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomException(RsCode.CHATROOM_NOT_FOUND));
        if (!conv.getUser().getUserId().equals(userId)) {
            throw new CustomException(RsCode.FORBIDDEN);
        }
        return conv;
    }
}