# API overview

## Base URL

The default local development base URL is:

```text
http://localhost:8080/api/v1
```

In production, use your deployed instance URL.

## API categories

- [Authentication API](authentication.md)
- [Identity management API](identity.md)
- [Admin operations API](admin.md)
- [Public integration API](integration.md)
- [Error model](errors.md)

## Authentication modes

### Bearer JWT

Used for user-facing protected endpoints.

```http
Authorization: Bearer <access_token>
```

### API key

Used only for trusted application endpoints.

```http
X-API-Key: <raw_api_key>
```

## Content type

Unless otherwise noted, requests and responses use JSON.

```http
Content-Type: application/json
Accept: application/json
```

## Pagination

Admin list endpoints return Spring Data `Page` payloads. Default page size is `20`.

Common query parameters:

- `page`
- `size`
- `sort`

## Time format

Date-time query parameters use ISO 8601 format, for example:

```text
2026-03-12T14:30:00Z
```

## Rate limiting

Implemented rate limits in the MVP:

- login: 5 requests per 15 minutes per matricule
- forgot password: 3 requests per hour per email
- global IP protection: 100 requests per minute per IP

When a limit is exceeded, the service returns HTTP `429 Too Many Requests`.

## Interactive API docs

- Swagger UI: `/api/v1/docs`
- OpenAPI JSON: `/api-docs`
