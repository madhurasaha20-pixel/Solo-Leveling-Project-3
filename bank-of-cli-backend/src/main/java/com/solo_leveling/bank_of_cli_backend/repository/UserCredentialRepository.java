package com.solo_leveling.bank_of_cli_backend.repository;

import com.solo_leveling.bank_of_cli_backend.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserCredential c where c.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
