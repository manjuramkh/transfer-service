package com.bank.transfer_service.domain;

public enum TransferStatus {

    INITIATED,

    DEBIT_IN_PROGRESS,

    DEBIT_SUCCESS,

    CREDIT_IN_PROGRESS,

    CREDIT_SUCCESS,

    COMPLETED,

    FAILED,

    ROLLED_BACK
}
