package com.solo_leveling.bank_of_cli_backend.service;

import com.solo_leveling.bank_of_cli_backend.repository.AccountRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Hands out public account numbers (ACC-1001, ACC-1002, ...) from the account_id_seq sequence in schema.sql.
 * The SQL comes from Hibernate's dialect, so it works on both H2 and Postgres.
 */
@Component
public class AccountNumberGenerator {
    static final String SEQUENCE = "account_id_seq";

    private final JdbcTemplate jdbc;
    private final AccountRepository accounts;
    private final String nextValueSql;

    public AccountNumberGenerator(JdbcTemplate jdbc, AccountRepository accounts, EntityManagerFactory emf) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.nextValueSql = emf.unwrap(SessionFactoryImplementor.class)
                .getJdbcServices().getDialect().getSequenceSupport()
                .getSequenceNextValString(SEQUENCE);
    }

    public String next() {
        // skip numbers already taken, e.g. by seed data in data.sql
        String id;
        do {
            Long number = jdbc.queryForObject(nextValueSql, Long.class);
            id = String.format(Locale.ROOT, "ACC-%04d", number);
        } while (accounts.existsById(id));
        return id;
    }
}
