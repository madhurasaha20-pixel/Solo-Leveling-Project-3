package com.solo_leveling.bank_of_cli_backend.repository;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
