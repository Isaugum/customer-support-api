package com.customersupport.websocket;

import com.customersupport.message.Message;
import com.customersupport.message.MessageRepository;
import com.customersupport.ticket.Ticket;
import com.customersupport.ticket.TicketRepository;
import com.customersupport.ticket.TicketStatus;
import com.customersupport.user.User;
import com.customersupport.user.UserRepository;
import com.customersupport.websocket.dto.ChatMessage;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ChatServiceTest {

    @Inject
    ChatService chatService;

    @InjectMock
    MessageRepository messageRepository;

    @InjectMock
    TicketRepository ticketRepository;

    @InjectMock
    UserRepository userRepository;

    // verifyAccess

    @Test
    void should_allowAccess_when_userIsTicketCreator() {
        // given
        User ticketOwner = new User();
        ticketOwner.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;
        ticket.user = ticketOwner;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatCode(() -> chatService.verifyAccess(1L, 1L)).doesNotThrowAnyException();
    }

    @Test
    void should_allowAccess_when_userIsAssignedOperator() {
        // given
        User ticketOwner = new User();
        ticketOwner.id = 1L;

        User operator = new User();
        operator.id = 3L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;
        ticket.user = ticketOwner;
        ticket.operator = operator;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatCode(() -> chatService.verifyAccess(1L, 3L)).doesNotThrowAnyException();
    }

    @Test
    void should_throwForbidden_when_userHasNoRelationToTicket() {
        // given
        User ticketOwner = new User();
        ticketOwner.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;
        ticket.user = ticketOwner;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> chatService.verifyAccess(1L, 99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throwForbidden_when_ticketIsClosed() {
        // given
        User ticketOwner = new User();
        ticketOwner.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.CLOSED;
        ticket.user = ticketOwner;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> chatService.verifyAccess(1L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throwForbidden_when_ticketIsArchived() {
        // given
        User ticketOwner = new User();
        ticketOwner.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ARCHIVED;
        ticket.user = ticketOwner;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> chatService.verifyAccess(1L, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throwNotFound_when_ticketDoesNotExistOnVerify() {
        // given
        when(ticketRepository.findByIdWithRelations(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> chatService.verifyAccess(999L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    // getHistory

    @Test
    void should_returnMessagesOrderedBySentAt_when_historyRequested() {
        // given
        User sender = new User();
        sender.id = 1L;

        Message msg1 = new Message();
        msg1.sender = sender;
        msg1.content = "first";
        msg1.sentAt = LocalDateTime.of(2024, 1, 1, 10, 0);

        Message msg2 = new Message();
        msg2.sender = sender;
        msg2.content = "second";
        msg2.sentAt = LocalDateTime.of(2024, 1, 1, 10, 5);

        when(messageRepository.findByTicketIdOrderBySentAt(1L)).thenReturn(List.of(msg1, msg2));

        // when
        List<ChatMessage> result = chatService.getHistory(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("first");
        assertThat(result.get(1).content()).isEqualTo("second");
    }

    @Test
    void should_returnEmptyList_when_ticketHasNoMessages() {
        // given
        when(messageRepository.findByTicketIdOrderBySentAt(1L)).thenReturn(List.of());

        // when
        List<ChatMessage> result = chatService.getHistory(1L);

        // then
        assertThat(result).isEmpty();
    }

    // persistMessage

    @Test
    void should_persistAndReturnMessage_when_ticketAndSenderExist() {
        // given
        Ticket ticket = new Ticket();
        ticket.id = 1L;

        User sender = new User();
        sender.id = 1L;

        when(ticketRepository.findByIdOptional(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(sender));

        // when
        ChatMessage result = chatService.persistMessage(1L, 1L, "Hello there");

        // then
        assertThat(result.senderId()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("Hello there");
        verify(messageRepository).persist(any(Message.class));
    }

    @Test
    void should_throwNotFound_when_ticketMissingOnPersist() {
        // given
        when(ticketRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> chatService.persistMessage(999L, 1L, "hello"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throwNotFound_when_senderMissingOnPersist() {
        // given
        Ticket ticket = new Ticket();
        ticket.id = 1L;

        when(ticketRepository.findByIdOptional(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> chatService.persistMessage(1L, 999L, "hello"))
                .isInstanceOf(NotFoundException.class);
    }
}
