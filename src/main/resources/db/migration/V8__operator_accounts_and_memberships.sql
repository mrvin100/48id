-- 48ID — V8: Operator accounts and memberships

CREATE TABLE operator_accounts (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique identifier for the operator account
    name        VARCHAR(100) NOT NULL UNIQUE,                       -- Human-readable name of the client application (e.g. "48Hub Team")
    description VARCHAR(500),                                       -- Optional description of the operator account
    created_by  UUID         REFERENCES users (id) ON DELETE SET NULL, -- Admin who created this account
    owned_api_key_id UUID,                                          -- Reference to the API key owned by this account (nullable, set after creation)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),                -- Timestamp when the account was created
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()                 -- Timestamp of last update
);

COMMENT ON TABLE operator_accounts IS 'Trusted client applications registered in the K48 ecosystem';

CREATE TABLE operator_memberships (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique identifier for the membership
    account_id  UUID        NOT NULL REFERENCES operator_accounts (id) ON DELETE CASCADE, -- The operator account this membership belongs to
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,             -- The 48ID user who is a member
    member_role VARCHAR(20) NOT NULL DEFAULT 'COLLABORATOR',                              -- Role within the account: OWNER or COLLABORATOR
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',                                   -- Membership lifecycle status: PENDING or ACTIVE
    invited_at  TIMESTAMPTZ NOT NULL DEFAULT now(),                                       -- Timestamp when the invitation was sent
    accepted_at TIMESTAMPTZ,                                                              -- Timestamp when the user accepted the invitation (null until accepted)
    CONSTRAINT uq_operator_memberships_account_user UNIQUE (account_id, user_id)
);

COMMENT ON TABLE operator_memberships IS 'Links 48ID users to operator accounts with a role and lifecycle status';

CREATE INDEX idx_operator_memberships_account_id ON operator_memberships (account_id);
CREATE INDEX idx_operator_memberships_user_id    ON operator_memberships (user_id);
CREATE INDEX idx_operator_memberships_account_role ON operator_memberships (account_id, member_role);
