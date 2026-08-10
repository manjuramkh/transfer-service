package com.bank.transfer_service.model;

import com.bank.transfer_service.domain.TransferStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class TransferResponse {

    private UUID transferId;

    private String transferReference;

    private UUID fromAccountId;

    private UUID toAccountId;

    private BigDecimal amount;

    private String currency;

    private String remarks;

    private TransferStatus status;

    private Instant createdAt;

    private Instant UpdatedAt;
}
