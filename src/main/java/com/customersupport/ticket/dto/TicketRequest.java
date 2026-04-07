package com.customersupport.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketRequest(
    @NotNull Long roomId,
    @NotBlank String initialMessage
) {}
