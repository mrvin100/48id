-- 48ID — V8: Add OPERATOR role
-- Story: WEB-S4-BE-05 — Seed OPERATOR role for read-only dashboard/audit/user access

INSERT INTO roles (name)
VALUES ('OPERATOR')
ON CONFLICT (name) DO NOTHING;
