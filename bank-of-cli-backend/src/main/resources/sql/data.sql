-- Runs on every startup, so each insert skips rows that already exist.
-- Standard MERGE works on both H2 and PostgreSQL (15+); ON CONFLICT is PostgreSQL-only.

--Create roles first.
MERGE INTO role r
USING (VALUES
    ('ROLE_CUSTOMER'),
    ('ROLE_ADMIN')
) AS v(name)
ON r.name = v.name
WHEN NOT MATCHED THEN
    INSERT (name) VALUES (v.name);


--Create dummy users.
MERGE INTO bank_user u
USING (VALUES
    (CAST('11111111-1111-4111-8111-111111111111' AS UUID),
     'Alice Johnson', 'alice@example.com'),

    (CAST('22222222-2222-4222-8222-222222222222' AS UUID),
     'Bob Smith', 'bob@example.com'),

    (CAST('33333333-3333-4333-8333-333333333333' AS UUID),
     'Charlie Davis', 'charlie@example.com')
) AS v(id, full_name, email)
ON u.id = v.id
WHEN NOT MATCHED THEN
    INSERT (id, full_name, email) VALUES (v.id, v.full_name, v.email);


--Create checking and savings accounts.
MERGE INTO account a
USING (VALUES
    ('ACC-1001',
     CAST('11111111-1111-4111-8111-111111111111' AS UUID),
     'checking', 'Everyday Checking', 2450.75, 'USD'),

    ('ACC-1002',
     CAST('11111111-1111-4111-8111-111111111111' AS UUID),
     'savings', 'Emergency Fund', 12500.00, 'USD'),

    ('ACC-2001',
     CAST('22222222-2222-4222-8222-222222222222' AS UUID),
     'checking', 'Main Checking', 875.20, 'USD'),

    ('ACC-2002',
     CAST('22222222-2222-4222-8222-222222222222' AS UUID),
     'savings', 'Vacation Savings', 3200.00, 'USD'),

    ('ACC-3001',
     CAST('33333333-3333-4333-8333-333333333333' AS UUID),
     'checking', 'Daily Spending', 1500.00, 'USD'),

    ('ACC-3002',
     CAST('33333333-3333-4333-8333-333333333333' AS UUID),
     'savings', 'Long-Term Savings', 9500.00, 'USD')
) AS v(id, user_id, account_type, nickname, balance, currency)
ON a.id = v.id
WHEN NOT MATCHED THEN
    INSERT (id, user_id, account_type, nickname, balance, currency)
    VALUES (v.id, v.user_id, v.account_type, v.nickname, v.balance, v.currency);
