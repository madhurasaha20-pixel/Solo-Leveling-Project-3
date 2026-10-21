package com.solo_leveling.bank_of_cli_backend.repository;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, String> {
    /** The user's accounts, oldest first. */
    @Query("select a from Account a where a.user.id = :userId order by a.createdAt, a.id")
    List<Account> findAllByOwner(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.user.id = :userId")
    List<Account> findAllByOwnerForUpdate(@Param("userId") UUID userId);

    /** Deletes all of the user's accounts. Delete their transactions first. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Account a where a.user.id = :userId")
    int deleteAllByOwner(@Param("userId") UUID userId);
}
