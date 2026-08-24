package com.bank.transfer_service.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AccountResponse {

    private UUID accountId;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus accountStatus;
    private boolean success = true;

}
