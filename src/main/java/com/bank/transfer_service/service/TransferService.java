package com.bank.transfer_service.service;

import com.bank.transfer_service.config.AccountServiceClient;
import com.bank.transfer_service.domain.Transfer;
import com.bank.transfer_service.domain.TransferStatus;
import com.bank.transfer_service.model.AccountResponse;
import com.bank.transfer_service.model.TransferDTO;
import com.bank.transfer_service.model.TransferRequest;
import com.bank.transfer_service.model.TransferResponse;
import com.bank.transfer_service.repos.TransferRepository;
import com.bank.transfer_service.util.NotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static org.hibernate.type.descriptor.java.JdbcDateJavaType.DATE_FORMAT;


@Slf4j
@Service
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountServiceClient accountClient;


    public TransferService(final TransferRepository transferRepository, final AccountServiceClient accountClient) {
        this.accountClient = accountClient;
        this.transferRepository = transferRepository;
    }

    public List<TransferResponse> findAll() {
        final List<Transfer> transfers = transferRepository.findAll(Sort.by("id"));

        List<TransferResponse> transferResponses = new ArrayList<>();

        for (Transfer transfer : transfers) {
            TransferResponse transferResponse = new TransferResponse();

            transferResponse.setTransferId(transfer.getId());
            transferResponse.setTransferReference(transfer.getTransferReference());
            transferResponse.setFromAccountId(transfer.getFromAccountId());
            transferResponse.setToAccountId(transfer.getToAccountId());
            transferResponse.setAmount(transfer.getAmount());
            transferResponse.setCurrency(transfer.getCurrency());
            transferResponse.setStatus(transfer.getStatus());
            transferResponse.setRemarks(transfer.getStatus() == TransferStatus.COMPLETED ? "Transfer completed successfully" : "Transfer failed");
            transferResponse.setCreatedAt(transfer.getInitiatedAt());
            transferResponse.setUpdatedAt(Instant.now());

            transferResponses.add(transferResponse);
        }

        return transferResponses;
    }

    public TransferDTO get(final UUID id) {
        return transferRepository.findById(id)
                .map(transfer -> mapToDTO(transfer, new TransferDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public TransferResponse create(final TransferRequest transferRequest) {

        validateAccounts(transferRequest.fromAccountId(), transferRequest.toAccountId());
        String transferReference = generateTransferReference();

        Transfer transfer = new Transfer();
        transfer.setTransferReference(transferReference);
        transfer.setFromAccountId(transferRequest.fromAccountId());
        transfer.setToAccountId(transferRequest.toAccountId());
        transfer.setAmount(transferRequest.amount());
        transfer.setCurrency(transferRequest.currency());
        transfer.setStatus(TransferStatus.INITIATED);
        transfer.setInitiatedAt(Instant.now());

        Transfer savedTransfer = transferRepository.save(transfer);

        TransferResponse transferResponse = new TransferResponse();
        transferResponse.setTransferId(savedTransfer.getId());
        transferResponse.setTransferReference(savedTransfer.getTransferReference());
        transferResponse.setFromAccountId(savedTransfer.getFromAccountId());
        transferResponse.setToAccountId(savedTransfer.getToAccountId());
        transferResponse.setAmount(savedTransfer.getAmount());
        transferResponse.setCurrency(savedTransfer.getCurrency());
        transferResponse.setStatus(savedTransfer.getStatus());
        transferResponse.setRemarks("Transfer initiated successfully");
        transferResponse.setCreatedAt(savedTransfer.getInitiatedAt());
        transferResponse.setUpdatedAt(Instant.now());

        return transferResponse;
    }

    private String generateTransferReference() {
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        return "TRF-" + date + "-" + code;

    }

    private Boolean validateAccounts(UUID senderAccount, UUID receiverAccount) {
        AccountResponse fromAccount = accountClient.getAccountId(senderAccount);
        AccountResponse toAccount = accountClient.getAccountId(receiverAccount);

        if (fromAccount == null) {
            throw new NotFoundException("Sender Account Not Found.");
        } if (toAccount == null) {
            throw new NotFoundException("Receiver Account Not Found.");
        }else {
            return true;
        }
    }

    public void update(final UUID id, final TransferDTO transferDTO) {
        final Transfer transfer = transferRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(transferDTO, transfer);
        transferRepository.save(transfer);
    }

    public void delete(final UUID id) {
        final Transfer transfer = transferRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        transferRepository.delete(transfer);
    }

    public List<TransferDTO> searchTransfers(final String fromAccountNumber, final String toAccountNumber, final TransferStatus status, Pageable pageable) {
        UUID fromId = null, toId = null;

        if(fromAccountNumber != null){
            AccountResponse fromAcc = accountClient.getTransferList(fromAccountNumber);
            if (fromAcc != null){
                fromId = fromAcc.getAccountId();
            }
        }

        if (toAccountNumber != null){
            AccountResponse toAcc = accountClient.getTransferList(toAccountNumber);
            if (toAcc != null){
                toId = toAcc.getAccountId();
            }
        }

        List<Transfer> byFromAccountIdOrToAccountId = transferRepository.findByFromAccountIdOrToAccountId(fromId, toId, pageable);

        List<TransferDTO> transferDTOS = byFromAccountIdOrToAccountId.stream().map(
                t -> mapToDTO(t, new TransferDTO())
        ).toList();

        return transferDTOS;

    }



    private TransferDTO mapToDTO(final Transfer transfer, final TransferDTO transferDTO) {

        transferDTO.setFromAccountId(transfer.getFromAccountId());
        transferDTO.setToAccountId(transfer.getToAccountId());
        transferDTO.setAmount(transfer.getAmount());
        transferDTO.setStatus(transfer.getStatus());
        transferDTO.setInitiatedAt(transfer.getInitiatedAt());
        return transferDTO;
    }

    private Transfer mapToEntity(final TransferDTO transferDTO, final Transfer transfer) {
        transfer.setFromAccountId(transferDTO.getFromAccountId());
        transfer.setToAccountId(transferDTO.getToAccountId());
        transfer.setAmount(transferDTO.getAmount());
        transfer.setStatus(transferDTO.getStatus());
        transfer.setInitiatedAt(transferDTO.getInitiatedAt());
        return transfer;
    }

}
