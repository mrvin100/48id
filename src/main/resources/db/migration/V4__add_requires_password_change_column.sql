-- ============================================
-- 48ID — V4: Add requires_password_change column
-- ============================================

-- Add requires_password_change column to users table
-- Default to TRUE for existing users to force password change on next login
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS requires_password_change BOOLEAN NOT NULL DEFAULT TRUE;

-- Add comment for documentation
COMMENT ON COLUMN users.requires_password_change IS 
    'Indicates whether user must change password on next login (e.g., after CSV provisioning)';
