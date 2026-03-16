# Environment Setup Guide

This guide explains how to configure 48ID for different environments (development, testing, production).

## Quick Start

```bash
# 1. Copy the environment template
cp .env.example .env

# 2. Start infrastructure (PostgreSQL + Redis)
docker compose up -d db redis

# 3. Run the application
./gradlew bootRun
```

That's it! The default configuration works for local development.

## Environment Profiles

48ID uses Spring profiles to handle environment-specific behavior. Set the profile using `SPRING_PROFILES_ACTIVE` in your `.env` file:

| Profile | Purpose | When to Use |
|---------|---------|-------------|
| **dev** | Local development | Default - relaxed security, Swagger enabled, detailed logs |
| **test** | Integration tests | CI/CD pipelines, automated testing |
| **prod** | Production | Live deployment - strict security, no Swagger |

## Configuration by Environment

### Development (Local)

**Profile:** `SPRING_PROFILES_ACTIVE=dev`

**What you get:**
- Swagger UI at `http://localhost:8080/swagger-ui.html`
- Detailed logging for debugging
- Relaxed CORS and security settings
- Mailpit for email testing (no real emails sent)

**Setup:**
```bash
# Use the defaults in .env.example
cp .env.example .env

# Start services (uses environment variables from .env)
docker compose up -d db redis mailpit

# Run application
./gradlew bootRun
```

### Testing (CI/CD)

**Profile:** `SPRING_PROFILES_ACTIVE=test`

**What you get:**
- Testcontainers for isolated database/Redis
- Mocked email service (no real emails)
- Disabled rate limiting for faster tests
- In-memory caching

**Setup:**
```bash
# Tests automatically use the test profile
./gradlew test

# No .env file needed - tests use Testcontainers
```

### Production

**Profile:** `SPRING_PROFILES_ACTIVE=prod`

**What you get:**
- Swagger disabled (security)
- Strict CORS validation
- Production logging (JSON format)
- Rate limiting enabled
- HTTPS enforcement

**Setup:**
```bash
# 1. Copy template
cp .env.example .env

# 2. Edit .env and set:
SPRING_PROFILES_ACTIVE=prod

# 3. Update these critical settings:
DATABASE_URL=jdbc:postgresql://your-db-host:5432/fortyeightid
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=<use-secret-manager>

REDIS_HOST=your-redis-host
REDIS_PASSWORD=<use-secret-manager>

MAIL_HOST=smtp.your-provider.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=<use-secret-manager>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_FROM=no-reply@yourdomain.com

JWT_ISSUER=https://id.yourdomain.com
CORS_ALLOWED_ORIGINS=https://yourdomain.com

SPRINGDOC_SWAGGER_ENABLED=false
OPENAPI_SERVERS=https://id.yourdomain.com

# 4. Generate production RSA keys (see below)
```


## RSA Key Generation (Production)

```bash
# 1. Copy the test template (optional - tests use Testcontainers)
cp .env.test.example .env.test

# 2. Run tests
./gradlew test
```

### Test Configuration

The test profile includes:

✅ **Testcontainers** — Automatic PostgreSQL and Redis provisioning  
✅ **Mock services** — Email service mocked, no real SMTP  
✅ **Disabled rate limiting** — Tests run without rate limit interference  
✅ **Swagger disabled** — No API docs in test mode  
✅ **Transactional rollback** — Database state cleaned after each test  

### Test-Specific Settings

```properties
# application-test.properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.clean-disabled=false
logging.level.io.k48.fortyeightid=DEBUG
```


## RSA Key Generation (Production)

For production, generate unique RSA keys (don't use the dev keys):

```bash
# Generate private key
openssl genrsa -out private.pem 2048

# Extract public key
openssl rsa -in private.pem -pubout -out public.pem

# Store securely in your secret manager or server
# Then reference them in .env:
JWT_RSA_PUBLIC_KEY=file:/path/to/public.pem
JWT_RSA_PRIVATE_KEY=file:/path/to/private.pem
```

## Secret Management (Production)

**Never commit production secrets to version control!**

For production, use environment variables or secret managers:

### Option 1: Environment Variables (Simple)

```bash
# Set directly on your server
export DATABASE_PASSWORD="your-secure-password"
export REDIS_PASSWORD="your-redis-password"
export MAIL_PASSWORD="your-smtp-password"

# Then run the app
./gradlew bootRun
```

### Option 2: Secret Managers (Recommended)

**AWS Secrets Manager:**
```bash
# Store secrets
aws secretsmanager create-secret \
  --name 48id/database-password \
  --secret-string "your-secure-password"

# Retrieve in startup script
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id 48id/database-password \
  --query SecretString \
  --output text)
```

**HashiCorp Vault:**
```bash
# Store secrets
vault kv put secret/48id \
  database-password="your-secure-password" \
  redis-password="your-redis-password"

# Retrieve in application
export DATABASE_PASSWORD=$(vault kv get -field=database-password secret/48id)
```

**Kubernetes Secrets:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: 48id-secrets
type: Opaque
stringData:
  database-password: your-secure-password
  redis-password: your-redis-password
  mail-password: your-smtp-password
```

## Environment Variables Reference

All available configuration options:

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes | `dev` | Environment profile (dev/test/prod) |
| `DATABASE_URL` | Yes | - | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Yes | - | Database username |
| `DATABASE_PASSWORD` | Yes | - | Database password |
| `REDIS_HOST` | Yes | `localhost` | Redis hostname |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | No | - | Redis password (if auth enabled) |
| `MAIL_HOST` | Yes | - | SMTP server hostname |
| `MAIL_PORT` | Yes | - | SMTP server port |
| `MAIL_USERNAME` | No | - | SMTP username (if auth required) |
| `MAIL_PASSWORD` | No | - | SMTP password (if auth required) |
| `MAIL_SMTP_AUTH` | No | `false` | Enable SMTP authentication |
| `MAIL_SMTP_STARTTLS` | No | `false` | Enable STARTTLS |
| `MAIL_FROM` | Yes | - | From email address |
| `MAIL_LOGIN_URL` | Yes | - | Frontend login page URL |
| `MAIL_ACTIVATION_URL` | Yes | - | Frontend activation page URL |
| `MAIL_RESET_PASSWORD_URL` | Yes | - | Frontend password reset page URL |
| `JWT_ISSUER` | Yes | - | JWT issuer (your backend URL) |
| `JWT_ACCESS_TOKEN_EXPIRY` | No | `900` | Access token lifetime (seconds) |
| `JWT_REFRESH_TOKEN_EXPIRY` | No | `86400` | Refresh token lifetime (seconds) |
| `JWT_RSA_PUBLIC_KEY` | Yes | - | Path to RSA public key |
| `JWT_RSA_PRIVATE_KEY` | Yes | - | Path to RSA private key |
| `CORS_ALLOWED_ORIGINS` | Yes | - | Allowed CORS origins (comma-separated) |
| `SERVER_PORT` | No | `8080` | Server port |
| `API_PREFIX` | No | `/api/v1` | API path prefix |
| `SPRINGDOC_SWAGGER_ENABLED` | No | `true` | Enable Swagger UI |
| `OPENAPI_SERVERS` | No | - | OpenAPI server URLs |

## Troubleshooting

### Connection Refused (Database)

```bash
# Check if PostgreSQL is running
docker ps | grep 48id-postgres

# Check port mapping (should be 5433:5432)
docker compose ps

# Verify DATABASE_URL uses port 5433
echo $DATABASE_URL
```

### Redis Connection Failed

```bash
# Check if Redis is running
docker ps | grep redis

# Test connection
redis-cli -h localhost -p 6379 ping
# Should return: PONG
```

### Email Not Sending (Dev)

```bash
# Check if Mailpit is running
docker ps | grep mailpit

# Access Mailpit Web UI
open http://localhost:8025
```

### Application Won't Start

```bash
# Check logs for missing environment variables
./gradlew bootRun

# Verify .env file exists and is loaded
cat .env

# Check for syntax errors in .env
# (no spaces around =, no quotes unless needed)
```

## Next Steps

- **[Quick Start](quickstart.md)** — Get started with development
- **[Deployment Guide](deployment.md)** — Deploy to production
- **[Architecture](architecture.md)** — Understand the system design
- **[Contributing](../developers/contributing.md)** — Development workflow
