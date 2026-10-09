package com.bank.transfer_service.UnitTest;

import com.bank.transfer_service.config.AccountServiceClient;
import com.bank.transfer_service.domain.Transfer;
import com.bank.transfer_service.domain.TransferStatus;
import com.bank.transfer_service.events.TransferCompletedEvent;
import com.bank.transfer_service.events.TransferEventProducer;
import com.bank.transfer_service.exception.AccountException;
import com.bank.transfer_service.model.AccountResponse;
import com.bank.transfer_service.model.TransferDTO;
import com.bank.transfer_service.model.TransferRequest;
import com.bank.transfer_service.model.TransferResponse;
import com.bank.transfer_service.repos.TransferRepository;
import com.bank.transfer_service.service.TransferService;
import com.bank.transfer_service.util.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountServiceClient accountClient;

    @Mock
    private TransferEventProducer transferEventProducer;

    @InjectMocks
    private TransferService transferService;

    private UUID fromAccountId;
    private UUID toAccountId;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
        request = new TransferRequest(
                fromAccountId,
                toAccountId,
                new BigDecimal("125.50"),
                "USD",
                "Rent"
        );

    }

    @Test
    @DisplayName("Create transfer returns successful response and publishes event")
    void createReturnsSuccessfulResponseAndPublishesEvent() throws AccountException {
        stubAccountsExist();
        AccountResponse successfulOperation = new AccountResponse();
        when(accountClient.getDebitAccount(fromAccountId, request.amount()))
                .thenReturn(successfulOperation);

        when(accountClient.getCreditAccount(toAccountId, request.amount()))
                .thenReturn(successfulOperation);

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);

            if (transfer.getId() == null) {
                transfer.setId(UUID.randomUUID());
            }

            return transfer;
        });

        TransferResponse response = transferService.create(request);

        assertNotNull(response.getTransferId());
        assertNotNull(response.getTransferReference());
        assertEquals(fromAccountId, response.getFromAccountId());
        assertEquals(toAccountId, response.getToAccountId());
        assertEquals(new BigDecimal("125.50"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(TransferStatus.CREDIT_SUCCESS, response.getStatus());
        assertEquals("Transfer initiated successfully", response.getRemarks());
        verify(transferRepository, times(2)).save(any(Transfer.class));
        verify(transferEventProducer).publishTransferCompleted(any(TransferCompletedEvent.class));
    }

    @Test
    @DisplayName("Create transfer marks transfer as failed when debit fails")
    void createMarksTransferFailedWhenDebitFails() {
        stubAccountsExist();
        AccountResponse failedOperation = new AccountResponse();
        failedOperation.setSuccess(false);
        when(accountClient.getDebitAccount(fromAccountId, request.amount()))
                .thenReturn(failedOperation);
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);

        AccountException exception = assertThrows(
                AccountException.class,
                () -> transferService.create(request)
        );

        assertEquals("Debit failed", exception.getMessage());
        verify(transferRepository, times(2)).save(transferCaptor.capture());
        assertEquals(TransferStatus.FAILED, transferCaptor.getValue().getStatus());
        verify(accountClient, never()).getCreditAccount(toAccountId, request.amount());
        verify(transferEventProducer, never()).publishTransferCompleted(any(TransferCompletedEvent.class));
    }

    @Test
    @DisplayName("Create transfer marks transfer as rolled back when credit fails")
    void createMarksTransferRolledBackWhenCreditFails() {
        stubAccountsExist();
        AccountResponse successfulDebit = new AccountResponse();
        AccountResponse failedCredit = new AccountResponse();
        failedCredit.setSuccess(false);
        when(accountClient.getDebitAccount(fromAccountId, request.amount()))
                .thenReturn(successfulDebit);
        when(accountClient.getCreditAccount(toAccountId, request.amount()))
                .thenReturn(failedCredit);
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);

        AccountException exception = assertThrows(
                AccountException.class,
                () -> transferService.create(request)
        );

        assertEquals("Credit Failed.", exception.getMessage());
        verify(transferRepository, times(2)).save(transferCaptor.capture());
        assertEquals(TransferStatus.ROLLED_BACK, transferCaptor.getValue().getStatus());
        verify(transferEventProducer, never()).publishTransferCompleted(any(TransferCompletedEvent.class));
    }

    @Test
    @DisplayName("Create transfer fails when sender account does not exist")
    void createFailsWhenSenderAccountDoesNotExist() {
        when(accountClient.getAccountId(fromAccountId)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> transferService.create(request));

        verify(transferRepository, never()).save(any(Transfer.class));
        verify(accountClient, never()).getDebitAccount(fromAccountId, request.amount());
        verify(transferEventProducer, never()).publishTransferCompleted(any(TransferCompletedEvent.class));
    }

    @Test
    @DisplayName("Create transfer fails when receiver account does not exist")
    void createFailsWhenReceiverAccountDoesNotExist() {
        when(accountClient.getAccountId(fromAccountId)).thenReturn(new AccountResponse());
        when(accountClient.getAccountId(toAccountId)).thenReturn(null);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> transferService.create(request)
        );

        assertEquals("Receiver Account Not Found.", exception.getMessage());
        verify(transferRepository, never()).save(any(Transfer.class));
        verify(accountClient, never()).getDebitAccount(fromAccountId, request.amount());
    }

    @Test
    @DisplayName("Find all transfers maps entity fields and completion remarks")
    void findAllMapsTransfersToResponses() {
        Instant initiatedAt = Instant.parse("2026-10-09T06:30:00Z");
        Transfer completed = transfer(fromAccountId, toAccountId, TransferStatus.COMPLETED, initiatedAt);
        completed.setId(UUID.randomUUID());
        completed.setTransferReference("TRF-20261009-AB12CD34");
        completed.setCurrency("USD");
        Transfer failed = transfer(fromAccountId, toAccountId, TransferStatus.FAILED, initiatedAt);
        failed.setId(UUID.randomUUID());
        failed.setCurrency("EUR");
        when(transferRepository.findAll(Sort.by("id"))).thenReturn(List.of(completed, failed));

        List<TransferResponse> responses = transferService.findAll();

        assertEquals(2, responses.size());
        assertEquals(completed.getId(), responses.get(0).getTransferId());
        assertEquals("TRF-20261009-AB12CD34", responses.get(0).getTransferReference());
        assertEquals(fromAccountId, responses.get(0).getFromAccountId());
        assertEquals(toAccountId, responses.get(0).getToAccountId());
        assertEquals(new BigDecimal("125.50"), responses.get(0).getAmount());
        assertEquals("USD", responses.get(0).getCurrency());
        assertEquals(TransferStatus.COMPLETED, responses.get(0).getStatus());
        assertEquals("Transfer completed successfully", responses.get(0).getRemarks());
        assertEquals(initiatedAt, responses.get(0).getCreatedAt());
        assertNotNull(responses.get(0).getUpdatedAt());
        assertEquals(TransferStatus.FAILED, responses.get(1).getStatus());
        assertEquals("Transfer failed", responses.get(1).getRemarks());
        verify(transferRepository).findAll(Sort.by("id"));
    }

    @Test
    @DisplayName("Find all transfers returns an empty list when none exist")
    void findAllReturnsEmptyList() {
        when(transferRepository.findAll(Sort.by("id"))).thenReturn(List.of());

        assertTrue(transferService.findAll().isEmpty());
    }

    @Test
    @DisplayName("Find all transfers propagates repository failures")
    void findAllPropagatesRepositoryFailure() {
        when(transferRepository.findAll(Sort.by("id")))
                .thenThrow(new IllegalStateException("Database unavailable"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transferService.findAll()
        );

        assertEquals("Database unavailable", exception.getMessage());
    }

    @Test
    @DisplayName("Get transfer maps an existing entity to a DTO")
    void getReturnsTransferDto() {
        Instant initiatedAt = Instant.parse("2026-10-09T06:30:00Z");
        Transfer transfer = transfer(fromAccountId, toAccountId, TransferStatus.CREDIT_SUCCESS, initiatedAt);
        UUID transferId = UUID.randomUUID();
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        TransferDTO result = transferService.get(transferId);

        assertEquals(fromAccountId, result.getFromAccountId());
        assertEquals(toAccountId, result.getToAccountId());
        assertEquals(new BigDecimal("125.50"), result.getAmount());
        assertEquals(TransferStatus.CREDIT_SUCCESS, result.getStatus());
        assertEquals(initiatedAt, result.getInitiatedAt());
        verify(transferRepository).findById(transferId);
    }

    @Test
    @DisplayName("Get transfer throws not found when the entity does not exist")
    void getThrowsNotFoundForMissingTransfer() {
        UUID transferId = UUID.randomUUID();
        when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transferService.get(transferId));
    }

    @Test
    @DisplayName("Update transfer copies DTO fields and saves the entity")
    void updateChangesAndSavesTransfer() {
        UUID transferId = UUID.randomUUID();
        Transfer existing = transfer(fromAccountId, toAccountId, TransferStatus.INITIATED, Instant.EPOCH);
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(existing));
        TransferDTO dto = new TransferDTO();
        dto.setFromAccountId(UUID.randomUUID());
        dto.setToAccountId(UUID.randomUUID());
        dto.setAmount(new BigDecimal("80.25"));
        dto.setStatus(TransferStatus.COMPLETED);
        dto.setInitiatedAt(Instant.parse("2026-10-09T07:00:00Z"));

        transferService.update(transferId, dto);

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(transferCaptor.capture());
        assertEquals(dto.getFromAccountId(), transferCaptor.getValue().getFromAccountId());
        assertEquals(dto.getToAccountId(), transferCaptor.getValue().getToAccountId());
        assertEquals(dto.getAmount(), transferCaptor.getValue().getAmount());
        assertEquals(dto.getStatus(), transferCaptor.getValue().getStatus());
        assertEquals(dto.getInitiatedAt(), transferCaptor.getValue().getInitiatedAt());
    }

    @Test
    @DisplayName("Update transfer throws not found when the entity does not exist")
    void updateThrowsNotFoundForMissingTransfer() {
        UUID transferId = UUID.randomUUID();
        when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transferService.update(transferId, new TransferDTO()));

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Delete removes an existing transfer")
    void deleteRemovesExistingTransfer() {
        UUID transferId = UUID.randomUUID();
        Transfer transfer = transfer(fromAccountId, toAccountId, TransferStatus.INITIATED, Instant.EPOCH);
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        transferService.delete(transferId);

        verify(transferRepository).delete(transfer);
    }

    @Test
    @DisplayName("Delete throws not found when the entity does not exist")
    void deleteThrowsNotFoundForMissingTransfer() {
        UUID transferId = UUID.randomUUID();
        when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transferService.delete(transferId));

        verify(transferRepository, never()).delete(any(Transfer.class));
    }

    @Test
    @DisplayName("Search transfers resolves account numbers and maps matching transfers")
    void searchTransfersResolvesAccountsAndMapsResults() {
        String fromAccountNumber = "FROM-001";
        String toAccountNumber = "TO-002";
        UUID resolvedFromId = UUID.randomUUID();
        UUID resolvedToId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        AccountResponse fromAccount = new AccountResponse();
        fromAccount.setAccountId(resolvedFromId);
        AccountResponse toAccount = new AccountResponse();
        toAccount.setAccountId(resolvedToId);
        when(accountClient.getTransferList(fromAccountNumber)).thenReturn(fromAccount);
        when(accountClient.getTransferList(toAccountNumber)).thenReturn(toAccount);
        Transfer transfer = transfer(resolvedFromId, resolvedToId, TransferStatus.COMPLETED, Instant.EPOCH);
        when(transferRepository.findByFromAccountIdOrToAccountId(resolvedFromId, resolvedToId, pageable))
                .thenReturn(List.of(transfer));

        List<TransferDTO> results = transferService.searchTransfers(
                fromAccountNumber, toAccountNumber, TransferStatus.COMPLETED, pageable
        );

        assertEquals(1, results.size());
        assertEquals(resolvedFromId, results.get(0).getFromAccountId());
        assertEquals(resolvedToId, results.get(0).getToAccountId());
        assertEquals(new BigDecimal("125.50"), results.get(0).getAmount());
        assertEquals(TransferStatus.COMPLETED, results.get(0).getStatus());
        verify(transferRepository).findByFromAccountIdOrToAccountId(resolvedFromId, resolvedToId, pageable);
    }

    @Test
    @DisplayName("Search transfers returns empty results when no matches exist")
    void searchTransfersReturnsEmptyListWhenNoMatchesExist() {
        Pageable pageable = PageRequest.of(0, 10);
        when(transferRepository.findByFromAccountIdOrToAccountId(null, null, pageable))
                .thenReturn(List.of());

        List<TransferDTO> results = transferService.searchTransfers(null, null, null, pageable);

        assertTrue(results.isEmpty());
        verify(transferRepository).findByFromAccountIdOrToAccountId(null, null, pageable);
    }

    @Test
    @DisplayName("Search transfers queries with a null account ID when account lookup fails")
    void searchTransfersHandlesUnknownAccountNumber() {
        String unknownAccountNumber = "UNKNOWN";
        Pageable pageable = PageRequest.of(0, 10);
        when(accountClient.getTransferList(unknownAccountNumber)).thenReturn(null);
        when(transferRepository.findByFromAccountIdOrToAccountId(isNull(), isNull(), eq(pageable)))
                .thenReturn(List.of());

        List<TransferDTO> results = transferService.searchTransfers(
                unknownAccountNumber, null, null, pageable
        );

        assertTrue(results.isEmpty());
        verify(transferRepository).findByFromAccountIdOrToAccountId(null, null, pageable);
    }

    @Test
    @DisplayName("Search transfers propagates account lookup failures")
    void searchTransfersPropagatesAccountLookupFailure() {
        Pageable pageable = PageRequest.of(0, 10);
        when(accountClient.getTransferList("FROM-001"))
                .thenThrow(new IllegalStateException("Account service unavailable"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transferService.searchTransfers("FROM-001", null, null, pageable)
        );

        assertEquals("Account service unavailable", exception.getMessage());
        verify(transferRepository, never()).findByFromAccountIdOrToAccountId(
                any(), any(), any(Pageable.class)
        );
    }

    private void stubAccountsExist() {
        when(accountClient.getAccountId(fromAccountId)).thenReturn(new AccountResponse());
        when(accountClient.getAccountId(toAccountId)).thenReturn(new AccountResponse());
    }

    private Transfer transfer(UUID fromId, UUID toId, TransferStatus status, Instant initiatedAt) {
        Transfer transfer = new Transfer();
        transfer.setFromAccountId(fromId);
        transfer.setToAccountId(toId);
        transfer.setAmount(new BigDecimal("125.50"));
        transfer.setStatus(status);
        transfer.setInitiatedAt(initiatedAt);
        return transfer;
    }
}
