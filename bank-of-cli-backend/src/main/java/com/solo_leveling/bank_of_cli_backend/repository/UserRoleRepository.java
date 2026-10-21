package com.solo_leveling.bank_of_cli_backend.repository;

import com.solo_leveling.bank_of_cli_backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    @EntityGraph(attributePaths = "role")
    List<UserRole> findByUserId(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserRole r where r.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
