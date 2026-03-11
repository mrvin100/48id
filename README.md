# 48id

Centralized identity and authentication platform for the K48 ecosystem

## Quick Start

### Prerequisites
- Java 21+
- Docker and Docker Compose
- PostgreSQL 15+
- Redis 7+

### Running Locally

1. Start infrastructure (PostgreSQL + Redis):
   ```bash
   docker compose up -d
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```

3. Access Swagger UI: http://localhost:8080/swagger-ui.html

## Documentation

- **[Integration Guide](INTEGRATION_GUIDE.md)** — How to integrate your application with 48ID
  - Pattern 1: Server-Side Rendering (Forward User JWT) — No API key required
  - Pattern 2: Token Verification (API Key + JWT) — For server-side validation
  - Pattern 3: Identity Lookup by ID — Get public profile by UUID
  - Pattern 4: Matricule Validation — Check if matricule exists
- **Swagger UI** — Interactive API documentation at http://localhost:8080/swagger-ui.html

## API Authentication

### API Key Authentication (Server-to-Server)

For external applications (48Hub, LP48, etc.):

```bash
curl -X GET http://localhost:8080/api/v1/admin/api-keys \
  -H "X-API-Key: your-api-key"
```

**Important:** All API key-protected endpoints require the `X-API-Key` header. See [Integration Guide](INTEGRATION_GUIDE.md) for details.

### JWT Bearer Token Authentication (User Authentication)

For end-user authentication:

```bash
curl -X GET http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer <jwt-token>"
```

**Note:** The `GET /api/v1/me` endpoint does NOT require an API key — it uses the user's JWT for authentication. This is the recommended pattern for server-side rendering.

## Development

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`. Migrations run automatically on startup.

## License

K48 Ecosystem — All rights reserved.
