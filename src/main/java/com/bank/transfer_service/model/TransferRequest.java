package com.bank.transfer_service.model;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String remarks
) { }
