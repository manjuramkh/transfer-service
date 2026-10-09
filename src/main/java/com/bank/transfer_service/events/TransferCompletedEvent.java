package com.bank.transfer_service.events;

import com.bank.transfer_service.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        String transferId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        Instant occurredAt
) {
}
