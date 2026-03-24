-- 48ID — V9: Operator Account & Membership
-- Story: WEB-S4-BE-07 — OperatorAccount & Membership

CREATE TABLE operator_accounts (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    owned_api_key_id UUID        REFERENCES api_keys (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE operator_memberships (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    operator_account_id UUID       NOT NULL REFERENCES operator_accounts (id) ON DELETE CASCADE,
    user_id            UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    member_role        VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (operator_account_id, user_id)
);

CREATE INDEX idx_operator_memberships_account ON operator_memberships (operator_account_id);
CREATE INDEX idx_operator_memberships_user    ON operator_memberships (user_id);

-- Add OPERATOR_INVITE purpose support (enum value documented here; enforced in application)
COMMENT ON COLUMN password_reset_tokens.purpose IS
    'Token purpose: PASSWORD_RESET | ACCOUNT_ACTIVATION | OPERATOR_INVITE';
