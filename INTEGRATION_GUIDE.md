# 48ID — Integration Guide

## Overview

48ID is the centralized identity and authentication platform for the K48 ecosystem. External applications (48Hub, LP48, and future platforms) integrate with 48ID to authenticate users and verify their identity.

## Authentication Methods

48ID supports two authentication methods:

1. **JWT Bearer Tokens** — For user authentication (recommended for user-facing flows)
2. **API Keys** — For server-to-server communication (machine-to-machine)

---

## API Key Authentication

### Obtaining an API Key

1. Contact K48 administration to request an API key for your application
2. Provide your application name and description
3. Admin will create the key and provide you with the raw key value
4. **Store the key securely** — it will only be shown once

### Using Your API Key

Include the API key in the `X-API-Key` header of your requests:

```bash
curl -X GET http://localhost:8080/api/v1/admin/api-keys \
  -H "X-API-Key: your-raw-api-key-here"
```

**Important:**
- Header name: `X-API-Key` (case-sensitive)
- Do NOT use the `Authorization` header for API keys
- API keys are for server-to-server communication only
- Never expose API keys in client-side code

### API Key Endpoints

All API key-protected endpoints require the `X-API-Key` header:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/verify-token` | Verify a user's JWT token |
| `GET` | `/api/v1/users/{id}/identity` | Get public identity claims |
| `GET` | `/api/v1/users/{matricule}/exists` | Check if matricule exists |

### Error Responses

**Missing API Key (HTTP 403):**
```json
{
  "error": "Missing X-API-Key header",
  "message": "Requires X-API-Key header. Contact K48 admin to obtain an API key."
}
```

**Invalid API Key (HTTP 403):**
```json
{
  "error": "Invalid API key",
  "message": "The provided API key is not valid or has been revoked."
}
```

---

## JWT Bearer Token Authentication

### User Login Flow

1. User submits credentials to `POST /api/v1/auth/login`
2. 48ID returns JWT access token and refresh token
3. Include access token in `Authorization: Bearer <token>` header
4. When access token expires, use refresh token to get new access token

### Using JWT Tokens

```bash
curl -X GET http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Quick Start: Verify a User Token

**Step 1:** Obtain an API key from K48 admin

**Step 2:** Receive JWT from user (after they log in via 48ID)

**Step 3:** Verify the JWT:

```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-token \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"token": "user-jwt-token-here"}'
```

**Success Response (HTTP 200):**
```json
{
  "valid": true,
  "user": {
    "id": "uuid",
    "matricule": "K48-2024-001",
    "name": "Ama Owusu",
    "email": "ama@k48.io",
    "role": "STUDENT",
    "batch": "2024",
    "specialization": "Software Engineering"
  }
}
```

**Expired Token Response (HTTP 200):**
```json
{
  "valid": false,
  "reason": "TOKEN_EXPIRED"
}
```

---

## Swagger UI Documentation

Interactive API documentation is available at: `http://localhost:8080/swagger-ui.html`

- All API key-protected endpoints are marked with 🔑 icon
- Click "Try it out" to test endpoints directly
- API key-protected endpoints show: "Requires X-API-Key header"

---

## Security Best Practices

1. **Never log API keys or JWT tokens** in application logs
2. **Store API keys in environment variables** or secure secret management
3. **Rotate API keys periodically** or if compromise is suspected
4. **Use HTTPS in production** — never send credentials over plain HTTP
5. **Implement rate limiting** on your application to prevent abuse

---

## Support

For API key requests or technical support, contact K48 administration.
