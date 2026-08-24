package com.bank.transfer_service.model;

import java.math.BigDecimal;

public record CreditRequest(
        BigDecimal amount
) {
}
