package com.solo_leveling.bank_of_cli_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {
    // public account number (e.g. ACC-1001), assigned when the account is created
    @Id
    @Column(updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private BankUser user;

    // "checking" or "savings" (CHECK constraint in schema.sql)
    @Column(name = "account_type", nullable = false)
    private String accountType;

    private String nickname;

    // never negative (CHECK constraint in schema.sql)
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency = "USD";

    // set by the database default
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
