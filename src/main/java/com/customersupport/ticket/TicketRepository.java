package com.customersupport.ticket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TicketRepository implements PanacheRepository<Ticket> {

    public List<Ticket> findAllWithRelations(TicketStatus status, Long roomId, Long operatorId,
            String sortField, boolean ascending) {
        String base = "select t from Ticket t left join fetch t.user left join fetch t.room left join fetch t.operator";
        String orderField = sortField.equals("updatedAt") ? "t.updatedAt" : "t.createdAt";
        String order = " order by " + orderField + " " + (ascending ? "asc" : "desc");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        conditions.add("t.status != :excludedStatus");
        params.put("excludedStatus", TicketStatus.ARCHIVED);

        if (status != null) {
            conditions.add("t.status = :status");
            params.put("status", status);
        }
        if (roomId != null) {
            conditions.add("t.room.id = :roomId");
            params.put("roomId", roomId);
        }
        if (operatorId != null) {
            conditions.add("t.operator.id = :operatorId");
            params.put("operatorId", operatorId);
        }

        String where = " where " + String.join(" and ", conditions);
        return find(base + where + order, params).list();
    }

    public Optional<Ticket> findByIdWithRelations(Long id) {
        return find(
                "select t from Ticket t left join fetch t.user left join fetch t.operator left join fetch t.room where t.id = ?1",
                id)
                .firstResultOptional();
    }

    public boolean hasOpenTicket(Long userId) {
        return count("user.id = ?1 and (status = ?2 or status = ?3)",
                userId, TicketStatus.WAITING, TicketStatus.ACTIVE) > 0;
    }
}
