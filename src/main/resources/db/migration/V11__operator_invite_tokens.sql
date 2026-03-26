-- V11: Dedicated operator invite tokens table
-- Replaces reuse of password_reset_tokens for OPERATOR_INVITE purpose

CREATE TABLE operator_invite_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(36) NOT NULL UNIQUE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id  UUID        NOT NULL REFERENCES operator_accounts(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_operator_invite_tokens_token ON operator_invite_tokens(token);
CREATE INDEX idx_operator_invite_tokens_user  ON operator_invite_tokens(user_id);
