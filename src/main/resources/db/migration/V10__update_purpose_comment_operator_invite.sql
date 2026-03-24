-- 48ID — V9: Update purpose comment to include OPERATOR_INVITE

COMMENT ON COLUMN password_reset_tokens.purpose IS
    'Token purpose: PASSWORD_RESET, ACCOUNT_ACTIVATION, or OPERATOR_INVITE';
