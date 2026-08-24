package com.bank.transfer_service.model;

import java.math.BigDecimal;

public record DebitRequest(
        BigDecimal amount
) { }
