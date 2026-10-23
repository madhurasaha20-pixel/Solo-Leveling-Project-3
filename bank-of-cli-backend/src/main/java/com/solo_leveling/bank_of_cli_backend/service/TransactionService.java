package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.dto.request.DepositRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.WithdrawRequest;
import com.solo_leveling.bank_of_cli_backend.dto.request.TransferRequest;
import com.solo_leveling.bank_of_cli_backend.dto.response.TransactionHistoryResponse;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;

import java.util.UUID;
import java.util.List;

public interface TransactionService {

    Transaction deposit(String accountId, UUID ownerId, DepositRequest request);
    Transaction withdraw(String accountId, UUID ownerId, WithdrawRequest request);
    Transaction transfer(String accountId, UUID ownerId, TransferRequest request);

    List<TransactionHistoryResponse> getHistory(String accountId, UUID ownerId, int limit);
}