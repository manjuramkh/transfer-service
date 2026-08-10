package com.bank.transfer_service.repos;

import com.bank.transfer_service.domain.Transfer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    List<Transfer> findByFromAccountIdOrToAccountId(UUID fromId, UUID toId, Pageable pageable);
}
