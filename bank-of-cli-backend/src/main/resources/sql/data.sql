-- runs on every startup, so keep inserts idempotent
--MERGE INTO role (name) KEY (name) VALUES ('ROLE_CUSTOMER');
INSERT INTO role (name)
VALUES ('ROLE_CUSTOMER')
ON CONFLICT (name) DO NOTHING;
