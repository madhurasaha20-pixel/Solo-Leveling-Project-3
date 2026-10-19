package com.solo_leveling.bank_of_cli_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}