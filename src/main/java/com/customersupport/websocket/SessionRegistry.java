package com.customersupport.websocket;

import com.customersupport.ticket.TicketClosedEvent;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SessionRegistry {

    private static final Logger LOG = Logger.getLogger(SessionRegistry.class);

    private final Map<String, Set<WebSocketConnection>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> connectionUsers = new ConcurrentHashMap<>();

    public void register(String ticketId, WebSocketConnection connection, Long userId) {
        sessions.computeIfAbsent(ticketId, k -> new CopyOnWriteArraySet<>()).add(connection);
        connectionUsers.put(connection.id(), userId);
        LOG.debugf("Registered session for ticket %s (userId=%d), total sessions: %d",
                ticketId, userId, sessions.get(ticketId).size());
    }

    public void unregister(String ticketId, WebSocketConnection connection) {
        Set<WebSocketConnection> ticketSessions = sessions.get(ticketId);
        if (ticketSessions != null) {
            ticketSessions.remove(connection);
            if (ticketSessions.isEmpty()) {
                sessions.remove(ticketId);
            }
        }
        connectionUsers.remove(connection.id());
    }

    public Long getUserId(String connectionId) {
        return connectionUsers.get(connectionId);
    }

    public void broadcast(String ticketId, String message) {
        Set<WebSocketConnection> ticketSessions = sessions.get(ticketId);
        if (ticketSessions != null) {
            ticketSessions.forEach(session -> session.sendText(message).subscribe().with(
                    ok -> {},
                    failure -> LOG.warnf("Failed to send message to session for ticket %s: %s",
                            ticketId, failure.getMessage())));
        }
    }

    void onTicketClosed(@Observes(during = TransactionPhase.AFTER_SUCCESS) TicketClosedEvent event) {
        String ticketId = event.ticketId();
        Set<WebSocketConnection> connections = sessions.remove(ticketId);
        if (connections == null) {
            return;
        }
        LOG.infof("Closing %d WebSocket session(s) for ticket %s", connections.size(), ticketId);
        for (WebSocketConnection conn : connections) {
            connectionUsers.remove(conn.id());
            conn.close().subscribe().with(
                    ok -> {},
                    err -> LOG.warnf("Failed to close session for ticket %s: %s", ticketId, err.getMessage()));
        }
    }
}
