package com.solo_leveling.bank_of_cli_backend.repository;

import com.solo_leveling.bank_of_cli_backend.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankUserRepository extends JpaRepository<BankUser, UUID> {
    Optional<BankUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from BankUser u where u.id = :userId")
    int deleteByIdNow(@Param("userId") UUID userId);
}
