package com.customersupport.ticket.dto;

import com.customersupport.ticket.Ticket;
import com.customersupport.ticket.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        Long roomId,
        Long userId,
        Long operatorId,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.id,
                ticket.room.id,
                ticket.user.id,
                ticket.operator != null ? ticket.operator.id : null,
                ticket.status,
                ticket.createdAt,
                ticket.updatedAt
        );
    }
}
