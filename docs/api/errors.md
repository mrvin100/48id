# Error model

## Standard API error format

Most endpoints return RFC 7807-style Problem Details for error cases.

Example:

```json
{
  "type": "https://48id.k48.io/errors/account-disabled",
  "title": "Account Disabled",
  "status": 401,
  "detail": "Your account is pending activation. Please activate it first.",
  "timestamp": "2026-03-12T14:46:39Z",
  "code": "ACCOUNT_NOT_ACTIVATED"
}
```

## Common error codes

| Code | Meaning | Typical status |
|---|---|---:|
| `INVALID_CREDENTIALS` | Matricule or password is invalid | 401 |
| `ACCOUNT_NOT_ACTIVATED` | User must activate account before login | 401 |
| `ACCOUNT_SUSPENDED` | User is suspended | 401 |
| `ACCOUNT_LOCKED` | Too many failed attempts or lock condition | 401 |
| `TOKEN_EXPIRED` | Access token expired | 401 |
| `TOKEN_INVALID` | JWT signature or structure invalid | 401 |
| `REFRESH_TOKEN_INVALID` | Refresh token invalid or expired | 401 |
| `PASSWORD_POLICY_VIOLATION` | Password does not meet policy | 400 |
| `NEW_PASSWORD_SAME_AS_CURRENT` | New password equals current password | 400 |
| `RESET_TOKEN_INVALID` | Reset or activation token invalid | 400 |
| `RESET_TOKEN_EXPIRED` | Reset or activation token expired | 400 |
| `ACCESS_DENIED` | Authenticated user lacks permission | 403 |

## Validation errors

Validation failures return `400` and include `violations`.

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

## API-key-protected endpoint errors

Trusted application endpoints may return a simpler JSON error when the `X-API-Key` header is missing or invalid.

```json
{
  "error": "Missing X-API-Key header",
  "message": "Requires X-API-Key header. Contact K48 admin to obtain an API key."
}
```

## CSV import error model

`POST /api/v1/admin/users/import` returns a domain-specific payload instead of Problem Details so row-level validation results stay explicit.

```json
{
  "imported": 0,
  "failed": 1,
  "errors": [
    {
      "row": 3,
      "matricule": "K48-2024-001",
      "error": "INVALID_EMAIL_FORMAT"
    }
  ]
}
```
