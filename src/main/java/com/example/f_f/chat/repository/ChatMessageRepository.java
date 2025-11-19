package com.example.f_f.chat.repository;

import com.example.f_f.chat.dto.ChatMessageDto;
import com.example.f_f.chat.entity.ChatMessage;
import com.example.f_f.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    //jpa를 바로 dto 형식으로 가져오기
    //해당하는 대화 창에 대한 대화 내용 싹 긁어오기
    @Query("""
           SELECT new com.example.f_f.chat.dto.ChatMessageDto(m.role, m.createdAt, m.content)
           FROM ChatMessage m
           WHERE m.conversation.id = :conversationId
           ORDER BY m.id ASC
           """)
    Page<ChatMessageDto> findMessageDtosByConversationId(Long conversationId, Pageable pageable);
}
