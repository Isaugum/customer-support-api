package com.customersupport.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.customersupport.message.Message;
import com.customersupport.message.MessageRepository;
import com.customersupport.room.Room;
import com.customersupport.room.RoomRepository;
import com.customersupport.ticket.dto.TicketRequest;
import com.customersupport.ticket.dto.TicketResponse;
import com.customersupport.user.User;
import com.customersupport.user.UserRepository;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@QuarkusTest
class TicketServiceTest {

    @Inject
    TicketService ticketService;

    @InjectMock
    TicketRepository ticketRepository;

    @InjectMock
    MessageRepository messageRepository;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    RoomRepository roomRepository;

    // getAllTickets

    @Test
    void should_returnTickets_when_noFiltersApplied() {
        // given
        when(ticketRepository.findAllWithRelations(null, null, null, "updatedAt", false))
                .thenReturn(List.of());

        // when
        List<TicketResponse> result = ticketService.getAllTickets(null, null, null, "updatedAt", "desc");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_throwBadRequest_when_sortFieldIsInvalid() {
        assertThatThrownBy(() -> ticketService.getAllTickets(null, null, null, "invalidField", "asc"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void should_throwBadRequest_when_orderValueIsInvalid() {
        assertThatThrownBy(() -> ticketService.getAllTickets(null, null, null, "createdAt", "sideways"))
                .isInstanceOf(BadRequestException.class);
    }

    // getTicketById

    @Test
    void should_returnTicket_when_idExists() {
        // given
        User user = new User();
        user.id = 1L;

        Room room = new Room();
        room.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.WAITING;
        ticket.user = user;
        ticket.room = room;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when
        TicketResponse result = ticketService.getTicketById(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void should_throwNotFound_when_ticketIdDoesNotExist() {
        assertThatThrownBy(() -> ticketService.getTicketById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    // createTicket

    @Test
    void should_createTicket_when_userAndRoomExist() {
        // given
        User user = new User();
        user.id = 1L;

        Room room = new Room();
        room.id = 1L;

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findByIdOptional(1L)).thenReturn(Optional.of(room));

        // when
        TicketResponse result = ticketService.createTicket(1L, new TicketRequest(1L, "I need help"));

        // then
        assertThat(result.status()).isEqualTo(TicketStatus.WAITING);
        assertThat(result.userId()).isEqualTo(1L);
        verify(messageRepository).persist(any(Message.class));
    }

    @Test
    void should_throwNotFound_when_userDoesNotExistOnCreate() {
        assertThatThrownBy(() -> ticketService.createTicket(999L, new TicketRequest(1L, "hello")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throwNotFound_when_roomDoesNotExistOnCreate() {
        // given
        User user = new User();
        user.id = 1L;

        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(user));

        // when / then
        assertThatThrownBy(() -> ticketService.createTicket(1L, new TicketRequest(999L, "hello")))
                .isInstanceOf(NotFoundException.class);
    }

    // takeTicket

    @Test
    void should_takeTicket_when_ticketIsWaiting() {
        // given
        User operator = new User();
        operator.id = 3L;

        User user = new User();
        user.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.WAITING;
        ticket.user = user;

        Room room = new Room();
        room.id = 1L;
        ticket.room = room;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdOptional(3L)).thenReturn(Optional.of(operator));

        // when
        TicketResponse result = ticketService.takeTicket(1L, 3L);

        // then
        assertThat(result.status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(result.operatorId()).isEqualTo(3L);
        assertThat(ticket.takenAt).isNotNull();
    }

    @Test
    void should_throwNotFound_when_ticketToTakeDoesNotExist() {
        assertThatThrownBy(() -> ticketService.takeTicket(999L, 3L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throwBadRequest_when_ticketIsNotWaiting() {
        // given
        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> ticketService.takeTicket(1L, 3L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void should_throwNotFound_when_operatorDoesNotExistOnTake() {
        // given
        User user = new User();
        user.id = 1L;

        Room room = new Room();
        room.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.WAITING;
        ticket.user = user;
        ticket.room = room;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> ticketService.takeTicket(1L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // closeTicket

    @Test
    void should_closeTicket_when_operatorIsAssigned() {
        // given
        User operator = new User();
        operator.id = 3L;

        User user = new User();
        user.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;
        ticket.operator = operator;
        ticket.user = user;

        Room room = new Room();
        room.id = 1L;
        ticket.room = room;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when
        TicketResponse result = ticketService.closeTicket(1L, 3L);

        // then
        assertThat(result.status()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    void should_throwNotFound_when_ticketToCloseDoesNotExist() {
        assertThatThrownBy(() -> ticketService.closeTicket(999L, 3L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throwBadRequest_when_ticketIsNotActive() {
        // given
        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.WAITING;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> ticketService.closeTicket(1L, 3L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void should_throwForbidden_when_operatorIsNotAssigned() {
        // given
        User assignedOperator = new User();
        assignedOperator.id = 3L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;
        ticket.operator = assignedOperator;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> ticketService.closeTicket(1L, 4L))
                .isInstanceOf(ForbiddenException.class);
    }

    // archiveTicket

    @Test
    void should_archiveTicket_when_ticketIsClosedAndOperatorIsAssigned() {
        // given
        User operator = new User();
        operator.id = 3L;

        User user = new User();
        user.id = 1L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.CLOSED;
        ticket.operator = operator;
        ticket.user = user;

        Room room = new Room();
        room.id = 1L;
        ticket.room = room;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when
        TicketResponse result = ticketService.archiveTicket(1L, 3L);

        // then
        assertThat(result.status()).isEqualTo(TicketStatus.ARCHIVED);
    }

    @Test
    void should_throwNotFound_when_ticketToArchiveDoesNotExist() {
        assertThatThrownBy(() -> ticketService.archiveTicket(999L, 3L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throwBadRequest_when_ticketIsNotClosedOnArchive() {
        // given
        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.ACTIVE;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> ticketService.archiveTicket(1L, 3L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void should_throwForbidden_when_operatorIsNotAssignedOnArchive() {
        // given
        User assignedOperator = new User();
        assignedOperator.id = 3L;

        Ticket ticket = new Ticket();
        ticket.id = 1L;
        ticket.status = TicketStatus.CLOSED;
        ticket.operator = assignedOperator;

        when(ticketRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(ticket));

        // when / then
        assertThatThrownBy(() -> ticketService.archiveTicket(1L, 4L))
                .isInstanceOf(ForbiddenException.class);
    }
}
