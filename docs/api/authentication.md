# Authentication API

Complete reference for authentication endpoints.

## POST /auth/login

Authenticate user with matricule and password.

### Request

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "matricule": "K48-2024-001",
  "password": "SecurePass@123"
}
```

### Response (200 OK)

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
  "token_type": "Bearer",
  "expires_in": 900,
  "requires_password_change": false,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "matricule": "K48-2024-001",
    "name": "John Doe",
    "role": "STUDENT",
    "batch": "2024",
    "specialization": "Software Engineering"
  }
}
```

### Error Responses

**400 Bad Request** - Validation Error
```json
{
  "type": "https://48id.k48.io/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "violations": [
    {
      "field": "matricule",
      "message": "Matricule is required"
    }
  ]
}
```

**401 Unauthorized** - Invalid Credentials
```json
{
  "type": "https://48id.k48.io/errors/invalid-credentials",
  "title": "Invalid Credentials",
  "status": 401,
  "detail": "Matricule or password is incorrect.",
  "code": "INVALID_CREDENTIALS"
}
```

**401 Unauthorized** - Account Suspended
```json
{
  "type": "https://48id.k48.io/errors/account-disabled",
  "title": "Account Disabled",
  "status": 401,
  "detail": "Your account has been suspended. Contact K48 administration.",
  "code": "ACCOUNT_SUSPENDED"
}
```

**423 Locked** - Account Locked
```json
{
  "type": "https://48id.k48.io/errors/account-locked",
  "title": "Account Locked",
  "status": 423,
  "detail": "Account temporarily locked due to too many failed attempts. Try again in 300 seconds.",
  "code": "ACCOUNT_LOCKED"
}
```

### Rate Limiting

- 5 requests per 15 minutes per matricule
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

---

## POST /auth/refresh

Exchange refresh token for new access token.

### Request

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Response (200 OK)

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "660e8400-e29b-41d4-a716-446655440001",
  "token_type": "Bearer",
  "expires_in": 900
}
```

**Note:** Refresh token is rotated on each use.

### Error Responses

**401 Unauthorized** - Invalid Refresh Token
```json
{
  "type": "https://48id.k48.io/errors/refresh-token-invalid",
  "title": "Refresh Token Invalid",
  "status": 401,
  "detail": "Refresh token invalid",
  "code": "REFRESH_TOKEN_INVALID"
}
```

---

## POST /auth/logout

Revoke refresh token and terminate session.

### Request

```http
POST /api/v1/auth/logout
Content-Type: application/json
```

```json
{
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Response (204 No Content)

No response body.

---

## POST /auth/forgot-password

Request password reset email.

### Request

```http
POST /api/v1/auth/forgot-password
Content-Type: application/json
```

```json
{
  "email": "student@k48.io"
}
```

### Response (200 OK)

```json
{
  "message": "If this email is registered, a password reset link has been sent."
}
```

**Note:** Always returns 200 to prevent email enumeration.

### Error Responses

**400 Bad Request** - Invalid Email Format
```json
{
  "type": "https://48id.k48.io/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "violations": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

---

## POST /auth/reset-password

Reset password using token from email.

### Request

```http
POST /api/v1/auth/reset-password
Content-Type: application/json
```

```json
{
  "token": "uuid-from-email",
  "newPassword": "NewSecure@456"
}
```

### Response (200 OK)

```json
{
  "message": "Password reset successful. Please log in with your new password."
}
```

### Error Responses

**400 Bad Request** - Invalid Token
```json
{
  "type": "https://48id.k48.io/errors/reset-token-invalid",
  "title": "Reset Token Invalid",
  "status": 400,
  "detail": "Invalid reset token.",
  "code": "RESET_TOKEN_INVALID"
}
```

**400 Bad Request** - Expired Token
```json
{
  "type": "https://48id.k48.io/errors/reset-token-expired",
  "title": "Reset Token Expired",
  "status": 400,
  "detail": "This reset link has expired. Please request a new one.",
  "code": "RESET_TOKEN_EXPIRED"
}
```

**400 Bad Request** - Password Policy Violation
```json
{
  "type": "https://48id.k48.io/errors/password-policy-violation",
  "title": "Password Policy Violation",
  "status": 400,
  "detail": "Password does not meet policy requirements",
  "violations": [
    {
      "field": "newPassword",
      "message": "Password must be at least 8 characters long"
    }
  ]
}
```

---

## POST /auth/change-password

Change password (authenticated user).

### Request

```http
POST /api/v1/auth/change-password
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

```json
{
  "currentPassword": "OldPass@123",
  "newPassword": "NewSecure@456"
}
```

### Response (200 OK)

No response body.

### Error Responses

**400 Bad Request** - Invalid Current Password
```json
{
  "type": "https://48id.k48.io/errors/invalid-credentials",
  "title": "Invalid Credentials",
  "status": 400,
  "detail": "Current password is incorrect.",
  "code": "INVALID_CREDENTIALS"
}
```

**400 Bad Request** - Same Password
```json
{
  "type": "https://48id.k48.io/errors/same-password",
  "title": "Same Password",
  "status": 400,
  "detail": "New password must be different from current password.",
  "code": "NEW_PASSWORD_SAME_AS_CURRENT"
}
```

---

## POST /auth/verify-token

Verify JWT token (external applications).

### Request

```http
POST /api/v1/auth/verify-token
X-API-Key: <api_key>
Content-Type: application/json
```

```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Response (200 OK) - Valid Token

```json
{
  "valid": true,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "matricule": "K48-2024-001",
    "name": "John Doe",
    "email": "john@k48.io",
    "role": "STUDENT",
    "batch": "2024",
    "specialization": "Software Engineering"
  }
}
```

### Response (200 OK) - Invalid Token

```json
{
  "valid": false,
  "reason": "TOKEN_EXPIRED"
}
```

**Possible reasons:**
- `TOKEN_EXPIRED` - Token has expired
- `TOKEN_INVALID` - Token signature invalid
- `ACCOUNT_SUSPENDED` - User account suspended
- `USER_NOT_FOUND` - User not found

---

## Next Steps

- [User Management API](user-management.md) - User CRUD operations
- [Admin API](admin.md) - Administrative operations
- [Identity Verification API](identity-verification.md) - External app verification
