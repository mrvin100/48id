# Identity management API

## `GET /api/v1/me`

Return the authenticated user's current profile.

### Authentication

Bearer token required.

### Response `200`

```json
{
  "id": "bca4f2f6-3fef-4bc4-8d32-9ee6bd3728e3",
  "matricule": "K48-2024-001",
  "email": "student@k48.io",
  "name": "Ama Owusu",
  "phone": "+237600000000",
  "batch": "2024",
  "specialization": "Software Engineering",
  "status": "ACTIVE",
  "roles": ["STUDENT"],
  "profileCompleted": true,
  "createdAt": "2026-03-12T14:46:39Z",
  "updatedAt": "2026-03-12T14:46:39Z"
}
```

### Error responses

- `401` missing or invalid bearer token
- `404` user record not found

---

## `PUT /api/v1/me`

Update the authenticated user's self-service profile fields.

### Authentication

Bearer token required.

### Request body

```json
{
  "phone": "+237600000001",
  "specialization": "Backend Engineering"
}
```

### Response `200`

Returns the updated `MeResponse` payload.

### Notes

In the MVP, self-service updates are limited to:

- `phone`
- `specialization`

### Error responses

- `400` validation failure
- `401` missing or invalid bearer token
- `404` user record not found
