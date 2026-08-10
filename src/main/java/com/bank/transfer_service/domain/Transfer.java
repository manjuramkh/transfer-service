package com.bank.transfer_service.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Schema(description = "Represents a transfer between two accounts.")
@Entity
@Getter
@Setter
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column
    private String transferReference;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID fromAccountId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID toAccountId;

    @Column(length = 3)
    private String currency;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column
    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    @Column
    private Instant initiatedAt;

    @Column
    private Instant updatedAt;

}
