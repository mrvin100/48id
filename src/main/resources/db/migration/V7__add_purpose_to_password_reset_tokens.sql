-- 48ID — V7: Add purpose to password_reset_tokens

ALTER TABLE password_reset_tokens
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(30) NOT NULL DEFAULT 'PASSWORD_RESET';

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_purpose
    ON password_reset_tokens (user_id, purpose);

COMMENT ON COLUMN password_reset_tokens.purpose IS
    'Token purpose: PASSWORD_RESET or ACCOUNT_ACTIVATION';
