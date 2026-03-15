# Quick Start

Get 48ID running locally in under 5 minutes.

## Prerequisites

- **Java 21+** ([Download](https://adoptium.net/))
- **Docker & Docker Compose** ([Download](https://www.docker.com/))
- **SMTP server** (or use [Mailpit](https://mailpit.axllent.org/) for local testing)

## Step 1: Clone the repository

```bash
git clone https://github.com/mrvin100/48id.git
cd 48id
```

## Step 2: Configure environment

Copy the example environment file:

```bash
cp .env.example .env
```

This creates your local configuration with sensible defaults for development.

Edit `.env` for your local environment. Minimum configuration:

```env
# Database (docker-compose maps PostgreSQL to 5433)
DATABASE_URL=jdbc:postgresql://localhost:5433/fortyeightid
DATABASE_USERNAME=fortyeightid
DATABASE_PASSWORD=fortyeightid

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_ISSUER=http://localhost:8080
JWT_RSA_PUBLIC_KEY=classpath:keys/public.pem
JWT_RSA_PRIVATE_KEY=classpath:keys/private.pem

# Mail (use Mailpit for local dev)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_FROM=no-reply@48id.local
MAIL_LOGIN_URL=http://localhost:3000/login
MAIL_ACTIVATION_URL=http://localhost:3000/activate-account
MAIL_RESET_PASSWORD_URL=http://localhost:3000/reset-password
```

## Step 3: Start infrastructure

```bash
docker compose up -d postgres redis mailpit
```

This starts:
- PostgreSQL on `localhost:5433` (mapped to avoid conflicts with local installs)
- Redis on `localhost:6379`
- Mailpit (email testing) on `localhost:1025` (SMTP) and `localhost:8025` (Web UI)

> **Note:** The `docker-compose.yml` also defines an `app` service that builds and runs 48ID in a container. Use `docker compose up -d` (without specifying services) to start everything including the app container. For local development, starting only `postgres`, `redis`, and `mailpit` and running the app with Gradle is recommended.

## Step 4: Run the application

Linux/macOS:
```bash
./gradlew bootRun
```

Windows:
```powershell
.\gradlew.bat bootRun
```

The application will:
1. Connect to PostgreSQL on port `5433`
2. Run Flyway database migrations
3. Start the Spring Boot application on port `8080`

## Step 5: Verify it's working

Open these URLs in your browser:

- **Health check:** http://localhost:8080/actuator/health
- **Swagger UI:** http://localhost:8080/api/v1/docs
- **OpenAPI JSON:** http://localhost:8080/api-docs
- **JWKS:** http://localhost:8080/.well-known/jwks.json

You should see:

```json
// http://localhost:8080/actuator/health
{
  "status": "UP"
}
```

### Email Testing with Mailpit

Mailpit captures all outgoing emails for local testing:

- **SMTP Server:** `localhost:1025` (configured in `.env`)
- **Web UI:** http://localhost:8025

All activation emails, password reset emails, and other notifications will appear in the Mailpit web interface instead of being sent to real email addresses.

## Step 6: Test the API

### Create an admin user (one-time setup)

**Prerequisites:** Run this SQL **only after** starting the application at least once so that Flyway migrations have created the database schema (including the `users` table and `requires_password_change` column).

```sql
-- Connect to the database
\c fortyeightid

-- Insert admin role
INSERT INTO roles (id, name) VALUES (gen_random_uuid(), 'ADMIN');

-- Insert admin user
INSERT INTO users (id, matricule, email, name, password_hash, phone, batch, specialization, status, profile_completed, requires_password_change)
VALUES (
  gen_random_uuid(),
  'ADMIN-001',
  'admin@k48.io',
  'System Administrator',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhkO', -- password: admin123
  '+237600000000',
  '2024',
  'Administration',
  'ACTIVE',
  true,
  false
);

-- Link user to admin role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.matricule = 'ADMIN-001' AND r.name = 'ADMIN';
```

### Login with the admin user

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "matricule": "ADMIN-001",
    "password": "admin123"
  }'
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "...",
  "token_type": "Bearer",
  "expires_in": 900,
  "requires_password_change": false,
  "user": {
    "id": "...",
    "matricule": "ADMIN-001",
    "name": "System Administrator",
    "role": "ADMIN",
    "batch": "2024",
    "specialization": "Administration"
  }
}
```

### Get your profile

```bash
curl -X GET http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Step 7: Optional — Email Testing with Mailpit

Mailpit is already included in the docker-compose setup and provides a modern email testing interface:

- **Web UI:** http://localhost:8025
- **SMTP Server:** `localhost:1025` (already configured)

All emails (activation, password reset, etc.) will appear in the Mailpit web interface instead of being sent to real addresses.

- **SMTP server:** `localhost:1025`
- **Web UI:** http://localhost:8025

Update `.env`:
```env
MAIL_HOST=localhost
MAIL_PORT=1025
```

## Troubleshooting

### Port already in use

If port 8080 is already in use, change it in `.env`:
```env
SERVER_PORT=8081
```

### Database connection error

Make sure PostgreSQL is running:
```bash
docker ps | grep postgres
```

If not running:
```bash
docker compose up -d postgres
```

### Redis connection error

Make sure Redis is running:
```bash
docker ps | grep redis
```

If not running:
```bash
docker compose up -d redis
```

## Next Steps

- **[API Overview](../api/overview.md)** — Explore all available endpoints
- **[Authentication Guide](authentication.md)** — Understanding auth flows
- **[Integration Guide](integration.md)** — Integrate your application
- **[Contributing](../../CONTRIBUTING.md)** — Set up development environment
