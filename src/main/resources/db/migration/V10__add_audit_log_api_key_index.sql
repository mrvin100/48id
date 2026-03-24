-- Index on JSONB details->>'keyId' scoped to API_KEY_USED events
-- Avoids full table scans when querying API key traffic
CREATE INDEX IF NOT EXISTS idx_audit_log_api_key_used_key_id
    ON audit_log ((details->>'keyId'))
    WHERE action = 'API_KEY_USED';
