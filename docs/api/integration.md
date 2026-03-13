# Public integration API

These endpoints are intended for trusted backend applications and require an admin-managed API key in `X-API-Key`.

## Authentication

```http
X-API-Key: <raw_api_key>
```

If the header is missing or invalid, the API returns `403`.

---

## `POST /api/v1/auth/verify-token`

Validate a user access token and, when valid, return user claims and profile context for the consuming application.

### Request body

```json
{
  "token": "<access-token>"
}
```

### Response `200`

Valid token:

```json
{
  "valid": true,
  "user": {
    "id": "bca4f2f6-3fef-4bc4-8d32-9ee6bd3728e3",
    "matricule": "K48-2024-001",
    "name": "Ama Owusu",
    "email": "student@k48.io",
    "role": "STUDENT",
    "batch": "2024",
    "specialization": "Software Engineering"
  }
}
```

Invalid token:

```json
{
  "valid": false,
  "reason": "TOKEN_EXPIRED"
}
```

### Possible invalid reasons

- `TOKEN_EXPIRED`
- `TOKEN_INVALID`
- `USER_NOT_FOUND`
- `ACCOUNT_SUSPENDED`

### Error responses

- `403` missing or invalid API key

---

## `GET /api/v1/users/{id}/identity`

Return a minimal public identity payload for a known user ID.

### Path parameters

- `id` — user UUID

### Response `200`

```json
{
  "id": "bca4f2f6-3fef-4bc4-8d32-9ee6bd3728e3",
  "matricule": "K48-2024-001",
  "name": "Ama Owusu",
  "batch": "2024",
  "specialization": "Software Engineering",
  "profileCompleted": true
}
```

### Error responses

- `403` missing or invalid API key
- `404` user not found

---

## `GET /api/v1/users/{matricule}/exists`

Check whether a matricule exists and, if it does, return the account status.

### Path parameters

- `matricule` — user matricule string

### Response `200`

Existing user:

```json
{
  "exists": true,
  "status": "ACTIVE"
}
```

Unknown matricule:

```json
{
  "exists": false
}
```

### Error responses

- `403` missing or invalid API key
