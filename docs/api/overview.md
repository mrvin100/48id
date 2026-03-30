# API Overview

## Base URL

```
http://localhost:8080/api/v1
```

## API Categories

| Category | Path prefix | Auth |
|---|---|---|
| [Authentication](authentication.md) | `/auth/*` | None / Bearer |
| [Identity (self)](identity.md) | `/me` | Bearer |
| [Admin operations](admin.md) | `/admin/*` | Bearer + `ROLE_ADMIN` |
| [Operator operations](operator.md) | `/operator/*` | Bearer + `ROLE_OPERATOR` |
| [Integration (external)](integration.md) | `/auth/verify-token`, `/users/{id}/identity` | `X-API-Key` |
| [Error reference](errors.md) | — | — |

## Authentication Modes

### Bearer JWT
Used for all user-facing protected endpoints.
```http
Authorization: Bearer <access_token>
```

### API Key
Used for trusted backend-to-backend integration.
```http
X-API-Key: <raw_api_key>
```

## Role System

| Role | JWT claim | Access |
|---|---|---|
| `ADMIN` | `ROLE_ADMIN` | All admin endpoints |
| `STUDENT` | `ROLE_STUDENT` | Self-service endpoints (`/me`) |
| `STUDENT + OPERATOR` | `ROLE_STUDENT,ROLE_OPERATOR` | Student + all `/operator/*` endpoints |

> **Note**: JWT roles may be encoded as a comma-separated string in a single array element: `["STUDENT,ROLE_OPERATOR"]`. The 48ID-Web BFF handles this format by splitting on commas before role comparison.

## Pagination

All list endpoints return Spring Data `Page` objects:

```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

Common query parameters: `page` (0-indexed), `size` (default 20), `sort` (e.g., `createdAt,desc`).

## Error Format

RFC 7807 Problem Details:
```json
{
  "type": "https://48id.k48.io/errors/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "User is already an active member of this account",
  "instance": "/api/v1/operator/accounts/xxx/invite"
}
```

## Rate Limiting

| Endpoint | Limit |
|---|---|
| `POST /auth/login` | 5 requests / 15 min per matricule |
| `POST /auth/forgot-password` | 3 requests / hour per email/IP |
| Global | 100 requests / min per IP |

Response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

## Interactive Docs

- Swagger UI: `http://localhost:8080/api/v1/docs`
- OpenAPI JSON: `http://localhost:8080/api-docs`
