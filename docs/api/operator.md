# Operator API

Operator accounts allow 48ID students to create multi-tenant application integrations. A student who creates or joins an operator account receives `ROLE_OPERATOR` and can access operator-scoped endpoints.

All endpoints require bearer token with `ROLE_OPERATOR` claim.

---

## Account management

### `GET /api/v1/operator/accounts`

List all operator accounts the caller belongs to (as OWNER or COLLABORATOR).

**Response `200`:**
```json
[
  {
    "id": "account-uuid",
    "name": "48Hub Platform",
    "description": "Operator account for 48Hub",
    "memberRole": "OWNER",
    "memberStatus": "ACTIVE",
    "createdAt": "2026-03-12T14:46:39Z"
  }
]
```

---

### `POST /api/v1/operator/accounts`

Create a new operator account. Caller becomes OWNER. Backend assigns `ROLE_OPERATOR` to caller's roles.

**Request body:**
```json
{ "name": "48Hub Platform", "description": "Optional description" }
```

**Response `201`:** Created account object.

---

### `DELETE /api/v1/operator/accounts/{id}`

Delete an operator account. OWNER only. Returns `204 No Content`.

---

## Member management

### `GET /api/v1/operator/accounts/{id}/members`

List all members with enriched user data.

**Response `200`:**
```json
[
  {
    "id": "membership-uuid",
    "userId": "user-uuid",
    "matricule": "K48-B1-42",
    "name": "Jean Doe",
    "memberRole": "OWNER",
    "status": "ACTIVE",
    "createdAt": "2026-03-12T14:46:39Z"
  }
]
```

---

### `POST /api/v1/operator/accounts/{id}/invite`

Invite a student by matricule. OWNER only. Creates PENDING membership and sends invite email.

**Request body:**
```json
{ "matricule": "K48-B1-42" }
```

**Response `201`:** No body.

**Error responses:**
- `404` — No user found with that matricule
- `409 "already an active member"` — User is already active in this account
- `409 "yourself"` — Owner cannot invite themselves
- `409 (re-invite)` — PENDING membership exists; backend resends email (idempotent)
- `403` — Caller is not the OWNER

---

### `DELETE /api/v1/operator/accounts/{id}/members/{targetUserId}`

Remove a collaborator by their **userId** (not membership record ID). OWNER only. Cannot remove the OWNER.

**Response `204`:** No body.

**Error responses:**
- `409` — No membership found / cannot remove OWNER
- `403` — Caller is not the OWNER

---

## Operator invite acceptance

### `POST /api/v1/operator/accounts/accept-invite`

Accept an operator invite using a token from the invite email.

**Request body:**
```json
{ "token": "invite-token-from-email" }
```

**Response `200`:** Membership activated.

---

## API key management

### `GET /api/v1/operator/api-keys?accountId=`

Get API key metadata for the operator account. Key value is never returned after creation.

**Response `200`:**
```json
{
  "id": "key-uuid",
  "appName": "48Hub Production",
  "createdAt": "2026-03-12T14:46:39Z",
  "lastUsedAt": "2026-03-30T12:00:00Z"
}
```

---

### `POST /api/v1/operator/api-keys?accountId=`

Create a new API key for the operator account. OWNER only.

**Request body:**
```json
{ "applicationName": "48Hub Production", "description": "Optional" }
```

**Response `201`:**
```json
{ "id": "key-uuid", "appName": "48Hub Production", "key": "raw_key_shown_once", "createdAt": "..." }
```

⚠️ Raw key shown once only — save immediately.

---

### `PUT /api/v1/operator/api-keys/rotate?accountId=`

Rotate the API key. Previous key is immediately invalidated.

**Response `200`:** New key object (same shape as POST).

---

### `DELETE /api/v1/operator/api-keys?accountId=`

Revoke the API key. Returns `204 No Content`.

---

## Users (API Consumers)

### `GET /api/v1/operator/users?accountId=`

List 48ID users who authenticated externally via this operator account's API key. These are _consumers_ of the operator's platform — not account members.

Data is aggregated from audit log entries with action `API_KEY_USED`.

**Response `200` (paginated):**
```json
{
  "content": [
    {
      "userId": "user-uuid",
      "matricule": "K48-B1-5",
      "email": "user@k48.io",
      "name": "Kwame Mensah",
      "batch": "B1",
      "status": "ACTIVE",
      "totalCalls": 142,
      "firstSeen": "2026-03-15T10:00:00Z",
      "lastSeen": "2026-03-30T13:00:00Z"
    }
  ],
  "totalElements": 35,
  "totalPages": 2
}
```

---

## Traffic

### `GET /api/v1/operator/traffic?accountId=`

Returns traffic data for a specific operator account.

**Response `200`:**
```json
{
  "apiKeyCalls": [
    { "timestamp": "...", "ip": "1.2.3.4", "endpoint": "/api/v1/auth/verify-token", "method": "POST", "totalInWindow": 5 }
  ],
  "memberActions": [
    { "userId": "uuid", "matricule": "K48-B1-3", "action": "OPERATOR_MEMBER_REMOVED", "endpoint": "...", "timestamp": "..." }
  ],
  "generatedAt": "2026-03-30T14:00:00Z"
}
```

---

## Audit log

### `GET /api/v1/operator/audit-log?accountId=`

Returns paginated audit log scoped to the operator account.

Query params: `page`, `size`, `sort`, `startDate`, `endDate`, `action`
