package com.solo_leveling.bank_of_cli_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import com.solo_leveling.bank_of_cli_backend.entity.Transaction;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Transaction t where t.account.id in (select a.id from Account a where a.user.id = :userId)")
    int deleteAllByAccountOwner(@Param("userId") UUID userId);

    List<Transaction> findByAccount_IdOrderByCreatedAtDescIdDesc(String accountId, Pageable pageable);
}