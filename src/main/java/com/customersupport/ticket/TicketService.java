package com.customersupport.ticket;

import com.customersupport.message.Message;
import com.customersupport.message.MessageRepository;
import com.customersupport.room.Room;
import com.customersupport.room.RoomRepository;
import com.customersupport.ticket.dto.TicketRequest;
import com.customersupport.ticket.dto.TicketResponse;
import com.customersupport.user.User;
import com.customersupport.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TicketService {

    private static final Logger LOG = Logger.getLogger(TicketService.class);
    private static final Set<String> VALID_SORT_FIELDS = Set.of("createdAt", "updatedAt");
    private static final Set<String> VALID_ORDERS = Set.of("asc", "desc");

    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    @Inject
    Event<TicketClosedEvent> ticketClosedEvent;

    @Inject
    public TicketService(TicketRepository ticketRepository, MessageRepository messageRepository,
            UserRepository userRepository, RoomRepository roomRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    public List<TicketResponse> getAllTickets(TicketStatus status, Long roomId, Long operatorId,
            String sort, String order) {
        if (!VALID_SORT_FIELDS.contains(sort)) {
            throw new BadRequestException("Invalid sort field '" + sort + "'. Allowed: createdAt, updatedAt");
        }
        if (!VALID_ORDERS.contains(order)) {
            throw new BadRequestException("Invalid order '" + order + "'. Allowed: asc, desc");
        }
        return ticketRepository.findAllWithRelations(status, roomId, operatorId, sort, order.equals("asc")).stream()
                .map(TicketResponse::from)
                .toList();
    }

    public TicketResponse getTicketById(Long id) {
        return ticketRepository.findByIdWithRelations(id)
                .map(TicketResponse::from)
                .orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
    }

    @Transactional
    public TicketResponse createTicket(Long userId, TicketRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));

        Room room = roomRepository.findByIdOptional(request.roomId())
                .orElseThrow(() -> new NotFoundException("Room " + request.roomId() + " not found"));

        Ticket ticket = new Ticket();
        ticket.user = user;
        ticket.room = room;
        ticket.status = TicketStatus.WAITING;
        ticketRepository.persist(ticket);

        LOG.infof("Ticket %d created by user %d in room %d", ticket.id, userId, request.roomId());

        Message message = new Message();
        message.ticket = ticket;
        message.sender = user;
        message.content = request.initialMessage();
        messageRepository.persist(message);

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse takeTicket(Long ticketId, Long operatorId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));

        if (ticket.status != TicketStatus.WAITING) {
            throw new BadRequestException("Ticket " + ticketId + " is already taken");
        }

        User operator = userRepository.findByIdOptional(operatorId)
                .orElseThrow(() -> new NotFoundException("User " + operatorId + " not found"));

        ticket.operator = operator;
        ticket.status = TicketStatus.ACTIVE;
        ticket.takenAt = LocalDateTime.now();

        LOG.infof("Ticket %d taken by operator %d", ticketId, operatorId);

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse closeTicket(Long ticketId, Long operatorId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));

        validateActiveAndOwnership(ticket, operatorId, "close");
        ticket.status = TicketStatus.CLOSED;

        LOG.infof("Ticket %d closed by operator %d", ticketId, operatorId);

        ticketClosedEvent.fire(new TicketClosedEvent(String.valueOf(ticketId)));

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse archiveTicket(Long ticketId, Long operatorId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));

        if (ticket.status != TicketStatus.CLOSED) {
            throw new BadRequestException("Ticket " + ticketId + " must be CLOSED before it can be archived");
        }
        if (ticket.operator == null || !ticket.operator.id.equals(operatorId)) {
            throw new ForbiddenException("Only the assigned operator can archive ticket " + ticketId);
        }
        ticket.status = TicketStatus.ARCHIVED;

        LOG.infof("Ticket %d archived by operator %d", ticketId, operatorId);

        ticketClosedEvent.fire(new TicketClosedEvent(String.valueOf(ticketId)));

        return TicketResponse.from(ticket);
    }

    private void validateActiveAndOwnership(Ticket ticket, Long operatorId, String action) {
        if (ticket.status != TicketStatus.ACTIVE) {
            throw new BadRequestException("Ticket " + ticket.id + " is not active");
        }
        if (ticket.operator == null || !ticket.operator.id.equals(operatorId)) {
            throw new ForbiddenException("Only the assigned operator can " + action + " ticket " + ticket.id);
        }
    }
}
