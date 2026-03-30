# Admin operations API

All endpoints on this page require a bearer token for a user with the `ADMIN` role.

## User administration

### `GET /api/v1/admin/users`

Retrieve paginated list of users with optional filtering.

**Query parameters:**
- `page` — zero-indexed page number (default: 0)
- `size` — page size (default: 20)
- `sort` — sort criteria, e.g., `createdAt,desc`
- `status` — filter by status (`ACTIVE`, `PENDING_ACTIVATION`, `SUSPENDED`)
- `role` — filter by role (`ADMIN`, `STUDENT`)
- `search` — search by matricule or email

**Response `200`:**
```json
{
  "content": [
    {
      "id": "bca4f2f6-3fef-4bc4-8d32-9ee6bd3728e3",
      "matricule": "K48-B1-1",
      "email": "student@k48.io",
      "name": "Ama Owusu",
      "phone": "+237600000000",
      "batch": "B1",
      "specialization": "Software Engineering",
      "status": "ACTIVE",
      "roles": ["ROLE_STUDENT"],
      "createdAt": "2026-03-12T14:46:39Z",
      "updatedAt": "2026-03-12T14:46:39Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```

---

### `PUT /api/v1/admin/users/{id}/status`

Change user account status.

**Request body:**
```json
{ "status": "SUSPENDED" }
```

Valid statuses: `ACTIVE`, `SUSPENDED`, `PENDING_ACTIVATION`

---

### `POST /api/v1/admin/users/{id}/reset-password`

Force a password reset by sending reset email to user.

---

### `DELETE /api/v1/admin/users/{id}`

Delete a user account permanently. Returns `204 No Content`.

---

## CSV provisioning

### `GET /api/v1/admin/users/import/template`

Download CSV template for bulk user import.

**Response `200`:** CSV with headers: `matricule,email,name,phone,batch,specialization`

---

### `POST /api/v1/admin/users/import`

Import users from CSV file. Multipart form with `file` parameter.

**Validation rules:**
- `matricule` — required, unique, format `K48-B{batch}-{seq}`
- `email` — required, valid email format, must end with `@k48.io`, unique
- `name` — required
- `phone` — optional, no strict format validation at import time
- `batch` — required, format `B\d+` (e.g., B1, B2)
- `specialization` — required

**Response `200`:**
```json
{
  "imported": 45,
  "failed": 2,
  "errors": [
    { "row": 10, "matricule": "K48-B1-55", "error": "MATRICULE_ALREADY_EXISTS" },
    { "row": 25, "matricule": "K48-B1-70", "error": "INVALID_EMAIL_FORMAT" }
  ]
}
```

---

## Audit log

### `GET /api/v1/admin/audit-log`

Retrieve paginated audit log.

**Query parameters:** `page`, `size`, `sort`, `startDate`, `endDate`, `action`

**Response `200`:**
```json
{
  "content": [
    {
      "id": "abc123",
      "action": "USER_CREATED",
      "userId": "bca4f2f6-...",
      "details": "{}",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0...",
      "timestamp": "2026-03-12T14:46:39Z"
    }
  ],
  "totalElements": 500,
  "totalPages": 25
}
```

---

## Dashboard metrics (Admin only)

### `GET /api/v1/admin/dashboard/metrics`

Returns snapshot metrics: total users, active users, active sessions, pending activations.

### `GET /api/v1/admin/dashboard/login-activity`

Returns 7-day login activity data for charting.

### `GET /api/v1/admin/dashboard/recent-activity`

Returns most recent audit events.

### `GET /api/v1/admin/dashboard/traffic`

Returns `AggregatedTrafficView` — aggregated API key call and member action stats across **all operator accounts**.

**Response `200`:**
```json
{
  "accounts": [
    {
      "accountId": "uuid",
      "accountName": "48Hub Platform",
      "apiKeyTraffic": {
        "totalCalls": 1240,
        "last24h": 45,
        "lastCalledAt": "2026-03-30T13:00:00Z"
      },
      "memberActivity": {
        "totalActions": 230,
        "last24h": 12,
        "lastActionAt": "2026-03-30T12:00:00Z"
      }
    }
  ],
  "generatedAt": "2026-03-30T14:00:00Z"
}
```

---

## API key administration

### `GET /api/v1/admin/api-keys`

List all system API keys with metadata (key value never returned after creation).

### `POST /api/v1/admin/api-keys`

Create a new admin-managed API key.

**Request body:**
```json
{ "applicationName": "48Hub Integration", "description": "For 48Hub backend" }
```

**Response `201`:**
```json
{
  "id": "key-uuid",
  "applicationName": "48Hub Integration",
  "key": "48id_sk_xxx...",
  "createdAt": "2026-03-12T14:46:39Z"
}
```

⚠️ The raw key is only shown once — save immediately.

### `DELETE /api/v1/admin/api-keys/{id}`

Revoke an API key. Returns `204 No Content`.
