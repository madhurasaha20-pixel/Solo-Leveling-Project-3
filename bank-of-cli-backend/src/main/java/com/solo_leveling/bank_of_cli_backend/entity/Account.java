package com.solo_leveling.bank_of_cli_backend.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Check(constraints = "account_type IN ('checking', 'savings')")
    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Check(constraints = "balance >= 0")
    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "userId", nullable = false)
    private Long userId;

}
