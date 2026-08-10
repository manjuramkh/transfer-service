package com.bank.transfer_service.model;

import com.bank.transfer_service.domain.TransferStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TransferDTO {

    private UUID fromAccountId;

    private UUID toAccountId;

    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private TransferStatus status;

    private Instant initiatedAt;

}
