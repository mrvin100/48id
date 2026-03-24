-- ============================================
-- 48ID — V6: Add user_agent to audit_log
-- ============================================

-- Add user_agent column to audit_log table
-- Stores the HTTP User-Agent header for audit trail
ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN audit_log.user_agent IS 
    'HTTP User-Agent header from the request (for audit trail)';
