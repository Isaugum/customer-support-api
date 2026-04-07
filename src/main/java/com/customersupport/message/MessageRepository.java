package com.customersupport.message;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {

    public List<Message> findByTicketId(Long ticketId) {
        return list("ticket.id", ticketId);
    }

    public List<Message> findByTicketIdOrderBySentAt(Long ticketId) {
        return list("ticket.id = ?1 order by sentAt asc", ticketId);
    }
}
