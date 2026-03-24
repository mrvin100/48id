# Authentication API

This page documents the user authentication and credential lifecycle endpoints.

## `POST /api/v1/auth/login`

Authenticate a user with matricule and password.

### Authentication

Public endpoint.

### Rate limit

- 5 requests per 15 minutes per matricule
- global IP rate limiting also applies

### Request body

```json
{
  "matricule": "K48-2024-001",
  "password": "TempPass123!"
}
```

### Response `200`

```json
{
  "access_token": "<jwt>",
  "refresh_token": "<refresh-token>",
  "token_type": "Bearer",
  "expires_in": 900,
  "requires_password_change": true,
  "user": {
    "id": "bca4f2f6-3fef-4bc4-8d32-9ee6bd3728e3",
    "matricule": "K48-2024-001",
    "name": "Ama Owusu",
    "role": "STUDENT",
    "batch": "2024",
    "specialization": "Software Engineering"
  }
}
```

### Error responses

- `400` validation failure
- `401` invalid credentials
- `401` `ACCOUNT_NOT_ACTIVATED`
- `401` `ACCOUNT_SUSPENDED`
- `401` `ACCOUNT_LOCKED`
- `429` rate limit exceeded

---

## `POST /api/v1/auth/refresh`

Rotate a refresh token and issue a new access token.

### Authentication

Public endpoint.

### Request body

```json
{
  "refresh_token": "<refresh-token>"
}
```

### Response `200`

```json
{
  "access_token": "<jwt>",
  "refresh_token": "<new-refresh-token>",
  "token_type": "Bearer",
  "expires_in": 900
}
```

### Error responses

- `401` `REFRESH_TOKEN_INVALID`

---

## `POST /api/v1/auth/logout`

Revoke the supplied refresh token.

### Authentication

Authenticated user token required.

### Request body

```json
{
  "refresh_token": "<refresh-token>"
}
```

### Response `204`

No body.

### Error responses

- `401` invalid or expired refresh token

---

## `POST /api/v1/auth/change-password`

Change the current authenticated user's password.

### Authentication

Bearer token required.

### Request body

```json
{
  "currentPassword": "TempPass123!",
  "newPassword": "NewSecure#2026"
}
```

### Response `200`

No body.

### Error responses

- `400` validation failure
- `400` `PASSWORD_POLICY_VIOLATION`
- `400` `NEW_PASSWORD_SAME_AS_CURRENT`
- `401` invalid current password

---

## `POST /api/v1/auth/forgot-password`

Request a password reset email.

### Authentication

Public endpoint.

### Rate limit

- 3 requests per hour per email
- global IP rate limiting also applies

### Request body

```json
{
  "email": "student@k48.io"
}
```

### Response `200`

```json
{
  "message": "If this email is registered, a password reset link has been sent."
}
```

### Notes

This endpoint is enumeration-safe. It returns `200` even when no matching account exists.

### Error responses

- `400` invalid email format
- `429` rate limit exceeded

---

## `POST /api/v1/auth/reset-password`

Reset a password using a password reset token.

### Authentication

Public endpoint.

### Request body

```json
{
  "token": "<password-reset-token>",
  "newPassword": "NewSecure#2026"
}
```

### Response `200`

```json
{
  "message": "Password reset successful. Please log in with your new password."
}
```

### Error responses

- `400` validation failure
- `400` `RESET_TOKEN_INVALID`
- `400` `RESET_TOKEN_EXPIRED`
- `400` `PASSWORD_POLICY_VIOLATION`

---

## `POST /api/v1/auth/activate-account`

Activate a provisioned account using the activation token delivered by email.

### Authentication

Public endpoint.

### Request body

```json
{
  "token": "<activation-token>"
}
```

### Response `200`

```json
{
  "message": "Account activated successfully. You can now log in with your temporary password and change it on first login."
}
```

### Notes

Activation changes the user status from `PENDING_ACTIVATION` to `ACTIVE`. The temporary password remains in force until the first successful login and password change.

### Error responses

- `400` validation failure
- `400` `RESET_TOKEN_INVALID`
- `400` `RESET_TOKEN_EXPIRED`
- `401` if the account cannot be activated in its current state

---

## `GET /.well-known/jwks.json`

Publish the JSON Web Key Set used to validate issued JWTs.

### Authentication

Public endpoint.

### Response `200`

Returns a standard JWKS document.

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "...",
      "use": "sig",
      "alg": "RS256",
      "n": "...",
      "e": "AQAB"
    }
  ]
}
```
