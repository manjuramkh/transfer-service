package com.bank.transfer_service.IntegrationTest;

import com.bank.transfer_service.exception.AccountException;
import com.bank.transfer_service.exception.GlobalException;
import com.bank.transfer_service.domain.TransferStatus;
import com.bank.transfer_service.model.TransferRequest;
import com.bank.transfer_service.model.TransferResponse;
import com.bank.transfer_service.rest.TransferResource;
import com.bank.transfer_service.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(TransferResource.class)
@Import(GlobalException.class)
public class ControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TransferService transferService;

    @Test
    @DisplayName("POST /api/transfers - Success")
    void createTransferReturnsCreatedResponse() throws Exception {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        TransferRequest request = new TransferRequest(
                fromAccountId,
                toAccountId,
                new BigDecimal("125.50"),
                "USD",
                "Rent"
        );

        TransferResponse response = new TransferResponse();
        response.setTransferId(transferId);
        response.setTransferReference("TRF-20261009-AB12CD34");
        response.setFromAccountId(fromAccountId);
        response.setToAccountId(toAccountId);
        response.setAmount(new BigDecimal("125.50"));
        response.setCurrency("USD");
        response.setStatus(TransferStatus.CREDIT_SUCCESS);
        response.setRemarks("Transfer initiated successfully");
        response.setCreatedAt(Instant.parse("2026-10-09T06:30:00Z"));
        response.setUpdatedAt(Instant.parse("2026-10-09T06:30:01Z"));
        when(transferService.create(any(TransferRequest.class))).thenReturn(response);

        webTestClient.post()
                .uri("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.transferId").isEqualTo(transferId.toString())
                .jsonPath("$.fromAccountId").isEqualTo(fromAccountId.toString())
                .jsonPath("$.toAccountId").isEqualTo(toAccountId.toString())
                .jsonPath("$.amount").isEqualTo(125.50)
                .jsonPath("$.currency").isEqualTo("USD")
                .jsonPath("$.status").isEqualTo("CREDIT_SUCCESS")
                .jsonPath("$.remarks").isEqualTo("Transfer initiated successfully");


        verify(transferService).create(any(TransferRequest.class));
    }

    @Test
    @DisplayName("POST /api/transfers - Account Operation Failure")
    void createTransferReturnsNotFoundWhenAccountOperationFails() throws Exception {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("125.50"),
                "USD",
                "Rent"
        );
        when(transferService.create(any(TransferRequest.class)))
                .thenThrow(new AccountException("Debit failed"));

        webTestClient.post()
                .uri("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo(404)
                .jsonPath("$.errorMessage").isEqualTo("Debit failed")
                .jsonPath("$.timestamp").exists();
    }
}
