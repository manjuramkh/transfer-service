package com.bank.transfer_service.rest;

import com.bank.transfer_service.domain.TransferStatus;
import com.bank.transfer_service.exception.AccountException;
import com.bank.transfer_service.model.TransferDTO;
import com.bank.transfer_service.model.TransferRequest;
import com.bank.transfer_service.model.TransferResponse;
import com.bank.transfer_service.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Transfer Service",
        description = "Endpoints for managing transfers between accounts"
)
@RestController
@RequestMapping(value = "/api/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransferResource {

    private final TransferService transferService;

    public TransferResource(final TransferService transferService) {
        this.transferService = transferService;
    }

    @Operation(
            summary = "Get all transfers",
            description = "Retrieve a list of all transfers in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved list of transfers"
    )
    @GetMapping
    public ResponseEntity<List<TransferResponse>> getAllTransfers() {
        return ResponseEntity.ok(transferService.findAll());
    }

    @Operation(
            summary = "Get a transfer by ID",
            description = "Retrieve the details of a specific transfer by its ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the transfer"
    )
    @GetMapping("/{id}")
    public ResponseEntity<TransferDTO> getTransfer(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(transferService.get(id));
    }


    @Operation(
            summary = "Create a new transfer",
            description = "Create a new transfer between accounts"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Successfully created a new transfer"
    )
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@RequestBody @Valid final TransferRequest transferRequest) throws AccountException, AccountException {
        final TransferResponse response = transferService.create(transferRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update an existing transfer",
            description = "Update the details of an existing transfer"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully updated the transfer"
    )
    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateTransfer(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final TransferDTO transferDTO) {
        transferService.update(id, transferDTO);
        return ResponseEntity.ok(id);
    }

    @Operation(
            summary = "Delete a transfer",
            description = "Delete an existing transfer by its ID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Successfully deleted the transfer"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable(name = "id") final UUID id) {
        transferService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a transfer",
            description = "Delete an existing transfer by its ID"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Successfully deleted the transfer"
    )
    @GetMapping("/search")
    public ResponseEntity<List<TransferDTO>> searchTransfer(
            @RequestParam(required = false) String fromAccountNumber,
            @RequestParam(required = false) String toAccountNumber,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        List<TransferDTO> transfers = transferService.searchTransfers(fromAccountNumber, toAccountNumber, status, pageable);
        return ResponseEntity.ok(transfers);
    }
}
