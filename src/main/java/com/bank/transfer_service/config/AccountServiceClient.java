package com.bank.transfer_service.config;

import com.bank.transfer_service.model.AccountResponse;
import com.bank.transfer_service.model.CreditRequest;
import com.bank.transfer_service.model.DebitRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
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

    @CircuitBreaker(name = "accountService", fallbackMethod = "getCreditAccountFallBack")
    @Retry(name = "accountService")
    public AccountResponse getCreditAccount(UUID accountId, BigDecimal amount) {
        CreditRequest request = new CreditRequest(amount);

            return webClient.post()
                    .uri("/{accountId}/credit", accountId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getDebitAccountFallBack")
    public AccountResponse getDebitAccount(UUID accountId, BigDecimal amount) {
        DebitRequest request = new DebitRequest(amount);
            return webClient.post()
                    .uri("/{accountId}/debit", accountId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountByNumberFallback")
    public AccountResponse getTransferList(String accountNumber) {
            return webClient.get()
                    .uri("/number/{accountNumber}", accountNumber)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();

    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    public AccountResponse getAccountId(UUID accountId) {
            return webClient.get()
                    .uri("/{accountId}", accountId)
                    .retrieve()
                    .bodyToMono(AccountResponse.class)
                    .block();
    }

    public AccountResponse getAccountFallback(UUID accountId, Throwable throwable) {
        log.error("Fallback method called for accountId {}: {}", accountId, throwable.getMessage());
        AccountResponse response = new AccountResponse();
        response.setSuccess(false);
        return response; // or return a default AccountResponse if appropriate
    }

    public AccountResponse getAccountByNumberFallback(String accountNumber, Throwable throwable) {
        log.error("Fallback method called for accountNumber {}: {}", accountNumber, throwable.getMessage());
        return null;
    }


    public AccountResponse getCreditAccountFallBack(UUID id, BigDecimal amount, Throwable throwable){
        log.error("Fallback method called for accountId {}: {}", id, throwable.getMessage());
        AccountResponse response = new AccountResponse();
        response.setSuccess(false);
        return response;
    }

    public AccountResponse getDebitAccountFallBack(UUID id, BigDecimal amount, Throwable throwable){
        log.error("Fallback method called for accountId {}: {}", id, throwable.getMessage());
        AccountResponse response = new AccountResponse();
        response.setSuccess(false);
        return response;
    }
}

