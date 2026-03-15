# Environment Setup Guide

This guide explains how to configure 48ID for different environments following industry best practices.

## Overview

48ID supports three environment profiles:

| Profile | Purpose | Configuration File |
|---------|---------|-------------------|
| **dev** | Local development | `.env.dev.example` → `.env` |
| **test** | Integration tests, CI/CD | `.env.test.example` → `.env.test` |
| **prod** | Production deployment | `.env.prod.example` → `.env.prod` |

## Environment Selection

The active profile is controlled by the `SPRING_PROFILES_ACTIVE` environment variable:

```bash
# Development
export SPRING_PROFILES_ACTIVE=dev

# Test
export SPRING_PROFILES_ACTIVE=test

# Production
export SPRING_PROFILES_ACTIVE=prod
```

## Development Environment

### Quick Setup

```bash
# 1. Copy the development template
cp .env.dev.example .env

# 2. Start infrastructure
docker compose up -d postgres redis

# 3. (Optional) Start MailHog for email testing
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog

# 4. Run the application
./gradlew bootRun
```

### Development Configuration

The development profile includes:

✅ **Relaxed security** — Longer token lifetimes, relaxed rate limits  
✅ **Debug logging** — Detailed SQL queries, security events  
✅ **Swagger UI enabled** — Interactive API documentation at `/api/v1/docs`  
✅ **Local services** — PostgreSQL on `5433`, Redis on `6379`, MailHog on `1025`  
✅ **Test RSA keys** — Pre-generated keys in `src/main/resources/keys/`  

### Development-Specific Settings

```properties
# application-dev.properties
logging.level.io.k48.fortyeightid=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### MailHog for Email Testing

MailHog captures all outgoing emails for local testing:

- **SMTP:** `localhost:1025`
- **Web UI:** http://localhost:8025

All activation and password reset emails will appear in the MailHog UI.

---

## Test Environment

### Quick Setup

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

### CI/CD Integration

For GitHub Actions, GitLab CI, or other CI/CD pipelines:

```yaml
# .github/workflows/test.yml
- name: Run tests
  run: ./gradlew test
  env:
    SPRING_PROFILES_ACTIVE: test
```

Testcontainers will automatically:
1. Pull PostgreSQL and Redis Docker images
2. Start containers with random ports
3. Configure Spring Boot to use them
4. Clean up after tests complete

---

## Production Environment

### Security Checklist

Before deploying to production:

- [ ] Generate production RSA keys (see below)
- [ ] Store all secrets in a secret manager (AWS Secrets Manager, Vault, etc.)
- [ ] Use managed database service (AWS RDS, Cloud SQL, Azure Database)
- [ ] Use managed Redis service (ElastiCache, Cloud Memorystore, Azure Cache)
- [ ] Configure transactional email service (SendGrid, AWS SES, Mailgun)
- [ ] Enable HTTPS/TLS with valid certificates
- [ ] Disable Swagger UI (`SPRINGDOC_SWAGGER_ENABLED=false`)
- [ ] Configure production CORS origins
- [ ] Set up monitoring and alerting
- [ ] Enable database backups and point-in-time recovery
- [ ] Configure log aggregation (CloudWatch, ELK, Datadog)

### Generate Production RSA Keys

```bash
# Generate private key
openssl genrsa -out private.pem 2048

# Extract public key
openssl rsa -in private.pem -pubout -out public.pem

# Base64 encode for environment variables
cat private.pem | base64 | tr -d '\n' > private.base64
cat public.pem | base64 | tr -d '\n' > public.base64

# Store in secret manager
aws secretsmanager create-secret \
  --name 48id/jwt-private-key \
  --secret-string file://private.base64

aws secretsmanager create-secret \
  --name 48id/jwt-public-key \
  --secret-string file://public.base64

# Clean up local files
rm private.pem public.pem private.base64 public.base64
```

### Production Configuration

The production profile includes:

✅ **Strict security** — Short token lifetimes, strict rate limits  
✅ **Minimal logging** — INFO level, no sensitive data  
✅ **Swagger disabled** — No API documentation exposure  
✅ **Managed services** — External PostgreSQL, Redis, SMTP  
✅ **Production RSA keys** — Unique keys stored in secret manager  
✅ **Connection pooling** — Optimized HikariCP settings  
✅ **Health checks** — Liveness and readiness probes  

### Production-Specific Settings

```properties
# application-prod.properties (or via environment variables)
logging.level.root=INFO
logging.level.io.k48.fortyeightid=INFO
logging.level.org.springframework.security=WARN

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never

spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### Secret Management

**Never commit production secrets to version control!**

Use a secret manager:

#### AWS Secrets Manager

```bash
# Store database password
aws secretsmanager create-secret \
  --name 48id/database-password \
  --secret-string "your-strong-password"

# Retrieve in application
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id 48id/database-password \
  --query SecretString \
  --output text)
```

#### HashiCorp Vault

```bash
# Store secrets
vault kv put secret/48id \
  database-password="your-strong-password" \
  redis-password="your-redis-password"

# Retrieve in application
export DATABASE_PASSWORD=$(vault kv get -field=database-password secret/48id)
```

#### Kubernetes Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: 48id-secrets
type: Opaque
stringData:
  database-password: your-strong-password
  redis-password: your-redis-password
  jwt-private-key: base64-encoded-private-key
```

---

## Environment Variables Reference

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev`, `test`, `prod` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5433/fortyeightid` |
| `DATABASE_USERNAME` | Database username | `fortyeightid` |
| `DATABASE_PASSWORD` | Database password | `strong-password` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_ISSUER` | JWT issuer claim | `http://localhost:8080` |
| `JWT_RSA_PUBLIC_KEY` | RSA public key | `classpath:keys/public.pem` or base64 |
| `JWT_RSA_PRIVATE_KEY` | RSA private key | `classpath:keys/private.pem` or base64 |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP server port | `8080` |
| `API_PREFIX` | API path prefix | `/api/v1` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000` |
| `SPRINGDOC_SWAGGER_ENABLED` | Enable Swagger UI | `true` (dev), `false` (prod) |
| `JWT_ACCESS_TOKEN_EXPIRY` | Access token lifetime (seconds) | `900` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh token lifetime (seconds) | `86400` (1 day) |
| `RATE_LIMIT_LOGIN_ATTEMPTS` | Login attempts per window | `5` |
| `RATE_LIMIT_LOGIN_WINDOW` | Login rate limit window (seconds) | `900` (15 min) |

### Mail Configuration

| Variable | Description | Example |
|----------|-------------|---------|
| `MAIL_HOST` | SMTP hostname | `smtp.sendgrid.net` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | `apikey` |
| `MAIL_PASSWORD` | SMTP password | `sendgrid-api-key` |
| `MAIL_SMTP_AUTH` | Enable SMTP auth | `true` |
| `MAIL_SMTP_STARTTLS` | Enable STARTTLS | `true` |
| `MAIL_FROM` | From email address | `no-reply@k48.io` |
| `MAIL_LOGIN_URL` | Login page URL | `https://app.k48.io/login` |
| `MAIL_ACTIVATION_URL` | Activation page URL | `https://app.k48.io/activate-account` |
| `MAIL_RESET_PASSWORD_URL` | Password reset page URL | `https://app.k48.io/reset-password` |

---

## Profile-Specific Behavior

### Development Profile (`dev`)

```
✓ Swagger UI enabled at /api/v1/docs
✓ Detailed debug logging
✓ SQL query logging with formatting
✓ Relaxed rate limits
✓ Local test RSA keys
✓ MailHog for email testing
```

### Test Profile (`test`)

```
✓ Testcontainers auto-provisioning
✓ Mock email service
✓ Disabled rate limiting
✓ Transactional test rollback
✓ Debug logging for troubleshooting
✓ Swagger disabled
```

### Production Profile (`prod`)

```
✓ Swagger UI disabled
✓ Minimal INFO-level logging
✓ Strict rate limits
✓ Production RSA keys from secret manager
✓ Managed database and Redis
✓ Transactional email service
✓ Health checks only (no metrics exposure)
```

---

## Troubleshooting

### "Failed to load ApplicationContext"

**Cause:** Missing required environment variables

**Solution:** Ensure all required variables are set in your `.env` file

### "Connection refused" to PostgreSQL

**Cause:** PostgreSQL not running or wrong port

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Start PostgreSQL
docker compose up -d postgres

# Verify port mapping (should be 5433:5432)
docker compose ps
```

### "Unable to connect to Redis"

**Cause:** Redis not running

**Solution:**
```bash
# Start Redis
docker compose up -d redis

# Verify Redis is accessible
redis-cli -h localhost -p 6379 ping
```

### "JWT signature validation failed"

**Cause:** Mismatched public/private keys

**Solution:** Ensure `JWT_RSA_PUBLIC_KEY` and `JWT_RSA_PRIVATE_KEY` are a matching pair

### Profile not loading

**Cause:** `SPRING_PROFILES_ACTIVE` not set or incorrect

**Solution:**
```bash
# Check current profile
echo $SPRING_PROFILES_ACTIVE

# Set profile
export SPRING_PROFILES_ACTIVE=dev

# Or in .env file
SPRING_PROFILES_ACTIVE=dev
```

---

## Best Practices

### 1. Never Commit Secrets

```bash
# Add to .gitignore
.env
.env.local
.env.*.local
*.pem
*.key
```

### 2. Use Environment-Specific Files

```
.env.dev.example    → .env (for local dev)
.env.test.example   → .env.test (for CI/CD)
.env.prod.example   → .env.prod (for production, stored in secret manager)
```

### 3. Validate Configuration on Startup

48ID validates required configuration on startup and fails fast if misconfigured.

### 4. Use Secret Managers in Production

Never use `.env` files in production. Use:
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault
- Google Secret Manager
- Kubernetes Secrets

### 5. Rotate Secrets Regularly

- Database passwords: Every 90 days
- Redis passwords: Every 90 days
- JWT RSA keys: Every 365 days
- API keys: On compromise or every 180 days

### 6. Monitor Configuration Changes

Log all configuration changes in production for audit trails.

---

## Next Steps

- **[Quick Start](quickstart.md)** — Get started with development
- **[Deployment Guide](deployment.md)** — Deploy to production
- **[Architecture](architecture.md)** — Understand the system design
- **[Contributing](../developers/contributing.md)** — Development workflow

