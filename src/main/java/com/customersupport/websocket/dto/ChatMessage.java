package com.customersupport.websocket.dto;

import java.time.LocalDateTime;

public record ChatMessage(
        Long senderId,
        String content,
        LocalDateTime sentAt
) {}
