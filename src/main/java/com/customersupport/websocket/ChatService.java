package com.customersupport.websocket;

import com.customersupport.message.Message;
import com.customersupport.message.MessageRepository;
import com.customersupport.ticket.Ticket;
import com.customersupport.ticket.TicketRepository;
import com.customersupport.ticket.TicketStatus;
import com.customersupport.user.User;
import com.customersupport.user.UserRepository;
import com.customersupport.websocket.dto.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class ChatService {

    private final MessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Inject
    public ChatService(MessageRepository messageRepository, TicketRepository ticketRepository,
                       UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void verifyAccess(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
        if (ticket.status == TicketStatus.CLOSED || ticket.status == TicketStatus.ARCHIVED) {
            throw new ForbiddenException("Ticket " + ticketId + " is no longer active");
        }
        boolean isTicketUser = ticket.user.id.equals(userId);
        boolean isAssignedOperator = ticket.operator != null && ticket.operator.id.equals(userId);
        if (!isTicketUser && !isAssignedOperator) {
            throw new ForbiddenException("User " + userId + " is not a participant of ticket " + ticketId);
        }
    }

    @Transactional
    public List<ChatMessage> getHistory(Long ticketId) {
        return messageRepository.findByTicketIdOrderBySentAt(ticketId).stream()
                .map(m -> new ChatMessage(m.sender.id, m.content, m.sentAt))
                .toList();
    }

    @Transactional
    public ChatMessage persistMessage(Long ticketId, Long senderId, String content) {
        Ticket ticket = ticketRepository.findByIdOptional(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
        User sender = userRepository.findByIdOptional(senderId)
                .orElseThrow(() -> new NotFoundException("User " + senderId + " not found"));

        Message message = new Message();
        message.ticket = ticket;
        message.sender = sender;
        message.content = content;
        messageRepository.persist(message);

        return new ChatMessage(senderId, content, message.sentAt);
    }
}
