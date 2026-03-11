-- ============================================
-- 48ID — V5: Add created_by to api_keys
-- ============================================

-- Add created_by column to api_keys table
-- References the users table for audit trail
ALTER TABLE api_keys
    ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);

-- Add index for efficient querying by creator
CREATE INDEX IF NOT EXISTS idx_api_keys_created_by ON api_keys (created_by);

-- Add comment for documentation
COMMENT ON COLUMN api_keys.created_by IS 
    'Admin user who created this API key (for audit trail)';
