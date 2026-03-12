# Authorization model

## Roles in the MVP

### `STUDENT`

A standard end user of K48 applications.

Typical permissions:

- authenticate
- refresh and revoke sessions
- view own profile
- update own profile
- change own password

### `ADMIN`

A privileged operator.

Typical permissions:

- list and retrieve users
- update user details
- change role and status
- unlock, soft-delete, and force-reset users
- import users from CSV
- access audit logs and login history
- create, rotate, and revoke API keys

### `API_CLIENT`

A technical role assigned at runtime to authenticated API keys.

Typical permissions:

- verify user tokens
- query limited public identity data
- check matricule existence

## Enforcement model

Authorization is implemented through Spring Security and method-level `@PreAuthorize` annotations.

Examples:

- admin controllers require `hasRole('ADMIN')`
- trusted integration endpoints require `hasRole('API_CLIENT')`

## Status-aware access control

Authorization is also constrained by user status:

- `PENDING_ACTIVATION` users cannot log in
- `SUSPENDED` users cannot authenticate successfully
- active users can authenticate and access permitted endpoints

## Design note

The MVP uses coarse-grained role checks. Fine-grained scopes or tenant-aware policies can be added in later phases without changing the documentation structure.
