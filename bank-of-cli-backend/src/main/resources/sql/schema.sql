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