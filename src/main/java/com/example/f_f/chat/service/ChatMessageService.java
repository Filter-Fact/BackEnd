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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatStoreService chatStoreService;
    private final WebClient fastApiClient;

    @Value("${fastapi.ask-path}")
    private String askPath;

    /**
     * AI 서버 호출 + 메시지 저장을 포함한 리액티브 흐름
     */
    public Mono<AnswerResponse> addAssistantMessage(Long conversationId, String userId, String userQuestion) {

        return Mono.fromCallable(() -> chatStoreService.saveUserMessage(conversationId, userId, userQuestion))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(conv ->
                        fastApiClient.post()
                                .uri(askPath)
                                .bodyValue(new UserQuestionDto(userQuestion))
                                .retrieve()
                                .bodyToMono(AnswerResponse.class)
                                .timeout(Duration.ofSeconds(30))
                )
                .flatMap(aiAnswer -> {
                    if (aiAnswer == null || aiAnswer.answer() == null || aiAnswer.answer().isBlank()) {
                        return Mono.error(new CustomException(RsCode.AI_EMPTY_RESPONSE));
                    }

                    return Mono.fromRunnable(() ->
                                    chatStoreService.saveAssistantMessage(conversationId, aiAnswer.answer())
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(aiAnswer);
                });
    }



    /**
     * 기존 페이지 조회는 동기 + 트랜잭션 유지
     */
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
