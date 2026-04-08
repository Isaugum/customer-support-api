package com.customersupport.websocket;

import com.customersupport.websocket.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@WebSocket(path = "/chat/{ticketId}")
public class ChatSocket {

    private static final Logger LOG = Logger.getLogger(ChatSocket.class);

    private final SessionRegistry sessionRegistry;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final JWTParser jwtParser;

    @Inject
    public ChatSocket(SessionRegistry sessionRegistry, ChatService chatService,
            ObjectMapper objectMapper, JWTParser jwtParser) {
        this.sessionRegistry = sessionRegistry;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.jwtParser = jwtParser;
    }

    @Blocking
    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        String ticketIdStr = connection.pathParam("ticketId");
        Long ticketId = Long.parseLong(ticketIdStr);

        String rawToken = extractCookie(connection.handshakeRequest().header("Cookie"), "jwt");
        if (rawToken == null || rawToken.isBlank()) {
            LOG.warnf("WebSocket connection without credentials for ticket %s — closing", ticketIdStr);
            connection.closeAndAwait();
            return;
        }

        Long userId;
        try {
            JsonWebToken jwt = jwtParser.parse(rawToken);
            userId = Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            LOG.warnf("WebSocket connection with invalid token for ticket %s — closing", ticketIdStr);
            connection.closeAndAwait();
            return;
        }

        try {
            chatService.verifyAccess(ticketId, userId);
        } catch (Exception e) {
            LOG.warnf("WebSocket access denied for user %d on ticket %s: %s — closing",
                    userId, ticketIdStr, e.getMessage());
            connection.closeAndAwait();
            return;
        }

        sessionRegistry.register(ticketIdStr, connection, userId);
        LOG.infof("WebSocket authenticated for ticket %s by user %d", ticketIdStr, userId);

        List<ChatMessage> history = chatService.getHistory(ticketId);
        for (ChatMessage msg : history) {
            try {
                connection.sendTextAndAwait(objectMapper.writeValueAsString(msg));
            } catch (Exception e) {
                LOG.warnf("Failed to send history message to user %d on ticket %s", userId, ticketIdStr);
            }
        }
    }

    @Blocking
    @OnTextMessage
    public void onMessage(WebSocketConnection connection, String content) {
        String ticketIdStr = connection.pathParam("ticketId");
        Long ticketId = Long.parseLong(ticketIdStr);
        Long userId = sessionRegistry.getUserId(connection.id());

        if (userId == null) {
            LOG.warnf("Message from unregistered connection %s on ticket %s — ignoring",
                    connection.id(), ticketIdStr);
            return;
        }

        try {
            ChatMessage message = chatService.persistMessage(ticketId, userId, content);
            sessionRegistry.broadcast(ticketIdStr, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            LOG.errorf("Failed to process message on ticket %s from user %d: %s",
                    ticketIdStr, userId, e.getMessage());
        }
    }

    private static String extractCookie(String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && trimmed.substring(0, eq).trim().equals(name)) {
                return trimmed.substring(eq + 1).trim();
            }
        }
        return null;
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        String ticketIdStr = connection.pathParam("ticketId");
        sessionRegistry.unregister(ticketIdStr, connection);
        LOG.infof("WebSocket closed for ticket %s", ticketIdStr);
    }
}
