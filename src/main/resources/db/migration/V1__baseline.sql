-- ============================================
-- 48ID — V1 Baseline Schema
-- ============================================

-- Roles lookup table
CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Seed default roles
INSERT INTO roles (name) VALUES ('ADMIN'), ('STUDENT');

-- Users table
CREATE TABLE users (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    matricule         VARCHAR(50) NOT NULL UNIQUE,
    email             VARCHAR(255) NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    phone             VARCHAR(30),
    batch             VARCHAR(50),
    specialization    VARCHAR(100),
    password_hash     VARCHAR(255) NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING_ACTIVATION',
    profile_completed BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_batch  ON users (batch);

-- User-role join table
CREATE TABLE user_roles (
    user_id UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);

-- Audit log
CREATE TABLE audit_log (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         REFERENCES users (id) ON DELETE SET NULL,
    action     VARCHAR(100) NOT NULL,
    details    JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_user_id    ON audit_log (user_id);
CREATE INDEX idx_audit_log_action     ON audit_log (action);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

-- Spring Modulith event publication table
CREATE TABLE event_publication (
    id               UUID         PRIMARY KEY,
    listener_id      TEXT         NOT NULL,
    event_type       TEXT         NOT NULL,
    serialized_event TEXT         NOT NULL,
    publication_date TIMESTAMPTZ  NOT NULL,
    completion_date  TIMESTAMPTZ
);

CREATE INDEX idx_event_publication_by_completion_date ON event_publication (completion_date);
