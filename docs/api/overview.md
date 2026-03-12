# API Overview

48ID provides a RESTful API for all identity and authentication operations.

## Base URL

```
http://localhost:8080/api/v1
```

## API Versioning

All API endpoints are prefixed with `/api/v1/`. Future versions will use `/api/v2/`, etc.

## Authentication

### JWT Bearer Token (User Authentication)

For user-facing operations:

```
Authorization: Bearer <jwt_token>
```

### API Key (External Application Authentication)

For server-to-server operations:

```
X-API-Key: <api_key>
```

## Response Format

### Success Response

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "uuid-here",
  "token_type": "Bearer",
  "expires_in": 900
}
```

### Error Response (RFC 7807)

```json
{
  "type": "https://48id.k48.io/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "timestamp": "2024-01-15T12:30:00Z",
  "violations": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

## Rate Limiting

| Endpoint | Limit | Window |
|----------|-------|--------|
| POST /auth/login | 5 requests | 15 minutes (per matricule) |
| POST /auth/forgot-password | 3 requests | 1 hour (per IP) |
| Global (all endpoints) | 100 requests | 1 minute (per IP) |

### Rate Limit Headers

```
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 3
X-RateLimit-Reset: 1705320600
Retry-After: 300
```

## Common Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 204 | Success (No Content) |
| 400 | Bad Request (Validation Error) |
| 401 | Unauthorized (Invalid Credentials/Token) |
| 403 | Forbidden (Access Denied) |
| 404 | Not Found |
| 409 | Conflict (Duplicate Resource) |
| 423 | Locked (Account Locked) |
| 429 | Too Many Requests (Rate Limited) |
| 500 | Internal Server Error |

## API Endpoints by Category

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /auth/login | User login | None |
| POST | /auth/refresh | Refresh access token | Refresh Token |
| POST | /auth/logout | User logout | Refresh Token |
| POST | /auth/forgot-password | Request password reset | None |
| POST | /auth/reset-password | Reset password | Reset Token |
| POST | /auth/change-password | Change password | Bearer Token |
| POST | /auth/verify-token | Verify JWT token | API Key |

### User Management

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /me | Get current user profile | Bearer Token |
| PUT | /me | Update current user profile | Bearer Token |
| GET | /users/{id}/identity | Get public identity | API Key |
| GET | /users/{matricule}/exists | Check matricule exists | API Key |

### Admin Operations

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | /admin/users | List users | ADMIN + Bearer |
| GET | /admin/users/{id} | Get user details | ADMIN + Bearer |
| PUT | /admin/users/{id} | Update user | ADMIN + Bearer |
| DELETE | /admin/users/{id} | Suspend user | ADMIN + Bearer |
| POST | /admin/users/{id}/reset-password | Force password reset | ADMIN + Bearer |
| POST | /admin/users/import | CSV bulk import | ADMIN + Bearer |
| GET | /admin/api-keys | List API keys | ADMIN + Bearer |
| POST | /admin/api-keys | Create API key | ADMIN + Bearer |
| DELETE | /admin/api-keys/{id} | Revoke API key | ADMIN + Bearer |
| GET | /admin/audit-log | Query audit log | ADMIN + Bearer |

## Pagination

List endpoints support pagination:

```
GET /api/v1/admin/users?page=0&size=20
```

### Response Format

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

## Filtering

Some endpoints support filtering:

```
GET /api/v1/admin/users?status=ACTIVE&batch=2024
```

## Date Format

All dates use ISO 8601 format:

```
2024-01-15T12:30:00Z
```

## Next Steps

- [Authentication API](api/authentication.md) - Login, logout, token management
- [User Management API](api/user-management.md) - User CRUD operations
- [Admin API](api/admin.md) - Administrative operations
- [Error Handling](api/errors.md) - Error response format
