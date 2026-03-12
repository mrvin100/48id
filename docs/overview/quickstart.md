# Quick Start

Get up and running with 48ID in minutes.

## Prerequisites

- Java 21 or higher
- Docker Desktop installed
- Git installed

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/mrvin100/48id.git
cd 48id
```

### 2. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL 15 on port 5432
- Redis 7 on port 6379

### 3. Configure Environment

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/fortyeightid
DATABASE_USERNAME=fortyeightid
DATABASE_PASSWORD=fortyeightid

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration
JWT_ISSUER=http://localhost:8080
JWT_ACCESS_TOKEN_EXPIRY=900
JWT_REFRESH_TOKEN_EXPIRY=604800

# Mail Configuration
MAIL_HOST=localhost
MAIL_PORT=1025

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### 4. Run Application

```bash
./gradlew bootRun
```

### 5. Verify Installation

Open http://localhost:8080/actuator/health

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

## Access Documentation

### Swagger UI
http://localhost:8080/api/v1/docs

### API Specification
http://localhost:8080/api-docs

## First API Calls

### 1. Health Check

```bash
curl http://localhost:8080/actuator/health
```

### 2. Login (Admin User)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "matricule": "K48-ADMIN-001",
    "password": "admin-password"
  }'
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "uuid-here",
  "token_type": "Bearer",
  "expires_in": 900,
  "requires_password_change": false,
  "user": {
    "id": "uuid",
    "matricule": "K48-ADMIN-001",
    "name": "Admin User",
    "role": "ADMIN",
    "batch": null,
    "specialization": null
  }
}
```

### 3. Get Current User Profile

```bash
curl http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer <access_token>"
```

## Next Steps

- Read the [Integration Guide](../integration/getting-started.md) for external application integration
- Explore the [API Reference](../api/overview.md) for all available endpoints
- Review [Security Documentation](../security/overview.md) for security best practices

## Troubleshooting

### Port Already in Use

If ports 5432, 6379, or 8080 are in use:

1. Stop conflicting services
2. Or modify `docker-compose.yml` and `application.properties`

### Database Connection Failed

Ensure PostgreSQL is running:

```bash
docker compose ps
```

Restart if needed:

```bash
docker compose restart postgres
```

### Application Won't Start

Check logs:

```bash
./gradlew bootRun --stacktrace
```

Common issues:
- Database not running
- Invalid environment variables
- Port conflicts
