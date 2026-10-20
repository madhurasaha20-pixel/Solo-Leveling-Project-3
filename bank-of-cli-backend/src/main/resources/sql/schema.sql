CREATE TABLE IF NOT EXISTS bank_user (
    id         UUID PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_credential (
    user_id       UUID PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES bank_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE          -- e.g. ROLE_CUSTOMER, ROLE_ADMIN
);

CREATE TABLE IF NOT EXISTS user_role (
    id      UUID PRIMARY KEY,                 -- surrogate, immutable
    user_id UUID   NOT NULL,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES bank_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(id),
    UNIQUE (user_id, role_id)                 -- still prevent duplicate grants
);

CREATE TABLE IF NOT EXISTS account (
    id           VARCHAR(20)    PRIMARY KEY,   -- public account number, e.g. ACC-1001
    user_id      UUID           NOT NULL,
    account_type VARCHAR(10)    NOT NULL CHECK (account_type IN ('checking', 'savings')),
    nickname     VARCHAR(30),                  -- optional, set by the owner
    balance      DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    currency     VARCHAR(3)     NOT NULL DEFAULT 'USD',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES bank_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bank_transaction (
    id               UUID PRIMARY KEY,
    account_id       VARCHAR(20)    NOT NULL,
    transaction_type VARCHAR(20)    NOT NULL CHECK (
        transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')
    ),
    amount           DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES account(id)
    );

--I added this for the account id generation part
CREATE SEQUENCE IF NOT EXISTS account_id_seq
    START WITH 1001
    INCREMENT BY 1;