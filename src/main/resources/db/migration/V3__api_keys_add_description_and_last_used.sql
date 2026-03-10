-- Add description and lastUsedAt fields to api_keys table
ALTER TABLE api_keys
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN last_used_at TIMESTAMPTZ;

CREATE INDEX idx_api_keys_last_used ON api_keys (last_used_at);
