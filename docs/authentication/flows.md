# Authentication flows

## Login flow

```mermaid
sequenceDiagram
    participant User
    participant App
    participant API as 48ID

    User->>App: Submit matricule and password
    App->>API: POST /api/v1/auth/login
    API->>API: Validate account status and password
    API-->>App: access_token + refresh_token + requires_password_change
    App-->>User: Start authenticated session
```

## Activation flow

Provisioned users are created in `PENDING_ACTIVATION` state. They cannot log in until activation succeeds.

```mermaid
sequenceDiagram
    participant Admin
    participant API as 48ID
    participant Mail
    participant User
    participant App

    Admin->>API: Import users via CSV
    API->>API: Create account with temp password
    API->>Mail: Send activation email
    Mail-->>User: Activation link + temporary password
    User->>App: Open activation link
    App->>API: POST /api/v1/auth/activate-account
    API->>API: Set status ACTIVE
    User->>App: Log in with temp password
    App->>API: POST /api/v1/auth/login
    API-->>App: requires_password_change=true
    App->>API: POST /api/v1/auth/change-password
```

## Refresh flow

```mermaid
sequenceDiagram
    participant App
    participant API as 48ID

    App->>API: POST /api/v1/auth/refresh
    API->>API: Validate refresh token
    API->>API: Rotate refresh token
    API-->>App: New access_token + refresh_token
```

## Password reset flow

```mermaid
sequenceDiagram
    participant User
    participant App
    participant API as 48ID
    participant Mail

    User->>App: Request password reset
    App->>API: POST /api/v1/auth/forgot-password
    API->>Mail: Send reset email if account exists
    Mail-->>User: Reset link
    User->>App: Submit new password with token
    App->>API: POST /api/v1/auth/reset-password
    API->>API: Invalidate token and revoke refresh tokens
```

## Session model

- access tokens are short-lived
- refresh tokens support session continuity
- logout revokes the supplied refresh token
- password reset revokes all refresh tokens for the user

## Password policy

The password policy is enforced server-side. The implementation validates password quality before change and reset operations and returns structured validation errors on failure.
