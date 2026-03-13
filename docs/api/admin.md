# Admin operations API

All endpoints on this page require a bearer token for a user with the `ADMIN` role.

## User administration

### `GET /api/v1/admin/users`

List users with optional filters.

#### Query parameters

- `status` — `ACTIVE`, `PENDING_ACTIVATION`, `SUSPENDED`
- `batch` — batch filter
- `role` — role filter
- `page`, `size`, `sort` — Spring Data paging controls

#### Response `200`

Returns a paginated Spring Data `Page<UserResponse>`.

---

### `GET /api/v1/admin/users/{id}`

Get a single user by ID.

### `PUT /api/v1/admin/users/{id}`

Update managed user fields.

#### Request body

```json
{
  "matricule": "K48-2024-001",
  "email": "student@k48.io",
  "name": "Ama Owusu",
  "phone": "+237600000000",
  "batch": "2024",
  "specialization": "Software Engineering"
}
```

### `PUT /api/v1/admin/users/{id}/role`

Change the user's role.

#### Request body

```json
{
  "role": "ADMIN"
}
```

### `PUT /api/v1/admin/users/{id}/status`

Change the user's status.

#### Request body

```json
{
  "status": "SUSPENDED"
}
```

### `POST /api/v1/admin/users/{id}/reset-password`

Trigger a password reset email for the target user.

#### Response `200`

```json
{
  "message": "Password reset email sent."
}
```

### `POST /api/v1/admin/users/{id}/unlock`

Unlock a locked account.

#### Response `200`

No body.

### `DELETE /api/v1/admin/users/{id}`

Soft-delete the target user.

#### Response `204`

No body.

### Common user-admin errors

- `400` validation failure
- `400` self-protection errors such as changing own role or deleting own account
- `401` invalid or missing bearer token
- `403` caller lacks `ADMIN`
- `404` user not found
- `409` duplicate matricule or duplicate email

---

## CSV provisioning

### `GET /api/v1/admin/users/import/template`

Download the CSV template used for bulk import.

#### Response `200`

Plain-text CSV content.

### `POST /api/v1/admin/users/import`

Import users from a CSV file upload.

#### Request

`multipart/form-data` with field name `file`.

#### Response `200`

```json
{
  "imported": 10,
  "failed": 2,
  "errors": [
    {
      "row": 5,
      "matricule": "K48-2024-004",
      "error": "INVALID_EMAIL_FORMAT"
    }
  ]
}
```

#### Response `400`

The same `CsvImportResult` shape is used for file-format or parsing failures.

---

## Audit log

### `GET /api/v1/admin/audit-log`

Retrieve paginated audit entries.

#### Query parameters

- `userId` — optional UUID filter
- `eventType` — optional event type filter
- `from` — optional ISO 8601 lower bound
- `to` — optional ISO 8601 upper bound
- `page`, `size`, `sort` — paging controls

### `GET /api/v1/admin/audit-log/login-history`

Retrieve paginated login-related audit entries for a specific user.

#### Query parameters

- `userId` — required UUID
- `from` — optional ISO 8601 lower bound
- `to` — optional ISO 8601 upper bound
- `page`, `size`, `sort` — paging controls

#### Audit response shape

```json
{
  "id": "fe736e9f-7ef6-47d6-98aa-6f12f06d52d9",
  "userId": "bca4f2f6-3fef-4bc4-8d32-9ee6bd3728e3",
  "action": "LOGIN_SUCCESS",
  "ipAddress": "127.0.0.1",
  "userAgent": "Mozilla/5.0",
  "timestamp": "2026-03-12T14:46:39Z"
}
```

---

## API key administration

### `POST /api/v1/admin/api-keys`

Create an API key for a trusted application.

#### Request body

```json
{
  "applicationName": "48Hub Backend",
  "description": "Token verification for 48Hub"
}
```

#### Response `201`

```json
{
  "key": "k48_live_xxx",
  "applicationName": "48Hub Backend",
  "createdAt": "2026-03-12T14:46:39Z"
}
```

> Store the raw key immediately. The raw value is only returned at creation and rotation time.

### `GET /api/v1/admin/api-keys`

List registered API keys.

### `POST /api/v1/admin/api-keys/{id}/rotate`

Rotate an existing API key and return a new raw secret.

### `DELETE /api/v1/admin/api-keys/{id}`

Revoke an API key.

#### Common API key admin errors

- `401` invalid or missing bearer token
- `403` caller lacks `ADMIN`
- `404` API key not found
