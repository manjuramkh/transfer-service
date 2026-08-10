package com.bank.transfer_service.config;

import com.bank.transfer_service.model.AccountResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Component
public class AccountServiceClient {

    private final WebClient webClient;

    private AccountResponse accountResponse;

    public AccountServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:7071/api/v1/accounts")
                .build();
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    @Retry(name = "accountService")
    public AccountResponse getCreditAccount(UUID accountId) {
        try {
            return webClient.get()
                    .uri("/{accountId}/credit", accountId)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error checking if account exists: {}", e.getMessage());
            return null;
        }
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    public AccountResponse getDebitAccount(UUID accountId) {
        try {
            return webClient.get()
                    .uri("/{accountId}/debit", accountId)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error checking if account exists: {}", e.getMessage());
            return null;
        }
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountByNumberFallback")
    public AccountResponse getTransferList(String accountNumber) {
        try {
            return webClient.get()
                    .uri("/number/{accountNumber}", accountNumber)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error checking if account exists: {}", e.getMessage());
            return null;
        }
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    public AccountResponse getAccountId(UUID accountId) {
        try {
            return webClient.get()
                    .uri("/{accountId}", accountId)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error checking if account exists: {}", e.getMessage());
            return null;
        }
    }

    public AccountResponse getAccountFallback(UUID accountId, Throwable throwable) {
        log.error("Fallback method called for accountId {}: {}", accountId, throwable.getMessage());
        return null; // or return a default AccountResponse if appropriate
    }

    public AccountResponse getAccountByNumberFallback(String accountNumber, Throwable throwable) {
        log.error("Fallback method called for accountNumber {}: {}", accountNumber, throwable.getMessage());
        return null;
    }
}

