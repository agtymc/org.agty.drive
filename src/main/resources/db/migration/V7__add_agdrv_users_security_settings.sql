ALTER TABLE public.agdrv_users
    ADD COLUMN IF NOT EXISTS two_factor_email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS two_factor_totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS two_factor_totp_secret VARCHAR(255),
    ADD COLUMN IF NOT EXISTS two_factor_totp_created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS two_factor_email_code_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS two_factor_email_code_expires_at TIMESTAMP;
