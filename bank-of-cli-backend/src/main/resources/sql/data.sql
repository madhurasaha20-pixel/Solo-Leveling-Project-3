--Create roles first.
INSERT INTO role (name)
VALUES
    ('ROLE_CUSTOMER'),
    ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;


--Create dummy users.
INSERT INTO bank_user (id, full_name, email)
VALUES
    ('11111111-1111-4111-8111-111111111111',
     'Alice Johnson', 'alice@example.com'),

    ('22222222-2222-4222-8222-222222222222',
     'Bob Smith', 'bob@example.com'),

    ('33333333-3333-4333-8333-333333333333',
     'Charlie Davis', 'charlie@example.com')
ON CONFLICT (id) DO NOTHING;


--Create checking and savings accounts.
INSERT INTO account
    (id, user_id, account_type, nickname, balance, currency)
VALUES
    ('ACC-1001',
     '11111111-1111-4111-8111-111111111111',
     'checking', 'Everyday Checking', 2450.75, 'USD'),

    ('ACC-1002',
     '11111111-1111-4111-8111-111111111111',
     'savings', 'Emergency Fund', 12500.00, 'USD'),

    ('ACC-2001',
     '22222222-2222-4222-8222-222222222222',
     'checking', 'Main Checking', 875.20, 'USD'),

    ('ACC-2002',
     '22222222-2222-4222-8222-222222222222',
     'savings', 'Vacation Savings', 3200.00, 'USD'),

    ('ACC-3001',
     '33333333-3333-4333-8333-333333333333',
     'checking', 'Daily Spending', 1500.00, 'USD'),

    ('ACC-3002',
     '33333333-3333-4333-8333-333333333333',
     'savings', 'Long-Term Savings', 9500.00, 'USD')
ON CONFLICT (id) DO NOTHING;