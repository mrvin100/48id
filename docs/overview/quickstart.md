# Quick start

## 1. Prerequisites

- Java 21
- Docker and Docker Compose
- SMTP server for email delivery, or a local mail catcher for development

## 2. Start local dependencies

```bash
docker compose up -d postgres redis
```

This starts:

- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

## 3. Configure environment variables

Copy `.env.example` and supply values appropriate for your environment.

Minimum variables for local development:

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/fortyeightid
DATABASE_USERNAME=fortyeightid
DATABASE_PASSWORD=fortyeightid
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_ISSUER=http://localhost:8080
JWT_RSA_PUBLIC_KEY=classpath:keys/public.pem
JWT_RSA_PRIVATE_KEY=classpath:keys/private.pem
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_FROM=no-reply@48id.k48.io
MAIL_LOGIN_URL=http://localhost:3000/login
MAIL_ACTIVATION_URL=http://localhost:3000/activate-account
MAIL_RESET_PASSWORD_URL=http://localhost:3000/reset-password
```

## 4. Run the application

Linux or macOS:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

## 5. Verify startup

Useful endpoints:

- Health: `GET /actuator/health`
- Swagger UI: `GET /api/v1/docs`
- OpenAPI JSON: `GET /api-docs`
- JWKS: `GET /.well-known/jwks.json`

## 6. Typical local workflow

1. Start dependencies with Docker Compose.
2. Run the application.
3. Open Swagger UI.
4. Provision users through the admin import API or seed data.
5. Activate a provisioned account from the activation email token.
6. Log in and test protected endpoints.

## 7. Suggested reading order

- [Architecture](architecture.md)
- [Authentication flows](../authentication/flows.md)
- [API overview](../api/overview.md)
- [Integration guide](../integration-guides/getting-started.md)
