-- =============================================================================
-- QuickStack POS - Database Migration V9
-- Module: Core
-- Description: Change INET columns to VARCHAR(45) to avoid Hibernate type issues
-- =============================================================================

ALTER TABLE users ALTER COLUMN last_login_ip TYPE VARCHAR(45);
-- created_ip does not exist on users table
-- ALTER TABLE users ALTER COLUMN created_ip TYPE VARCHAR(45); 

ALTER TABLE password_reset_tokens ALTER COLUMN created_ip TYPE VARCHAR(45);

ALTER TABLE login_attempts ALTER COLUMN ip_address TYPE VARCHAR(45);

ALTER TABLE refresh_tokens ALTER COLUMN ip_address TYPE VARCHAR(45);
