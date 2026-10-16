-- runs on every startup, so keep inserts idempotent
MERGE INTO role (name) KEY (name) VALUES ('ROLE_CUSTOMER');