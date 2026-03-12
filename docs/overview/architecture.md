# Architecture Overview

48ID uses a modular architecture built on Spring Boot 3 and Spring Modulith.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Applications                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │   48Hub     │  │    LP48     │  │  Future Applications    │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      48ID Platform                               │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                    API Layer                               │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │ │
│  │  │   Auth      │  │   User      │  │     Admin       │   │ │
│  │  │ Controller  │  │ Controller  │  │   Controller    │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘   │ │
│  └───────────────────────────────────────────────────────────┘ │
│                              │                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                   Service Layer                            │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │ │
│  │  │   Auth      │  │   User      │  │     Audit       │   │ │
│  │  │  Service    │  │  Service    │  │    Service      │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘   │ │
│  └───────────────────────────────────────────────────────────┘ │
│                              │                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                 Persistence Layer                          │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │ │
│  │  │ PostgreSQL  │  │   Redis     │  │    Flyway       │   │ │
│  │  │  (JPA)      │  │  (Cache)    │  │  (Migrations)   │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘   │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Module Structure

48ID uses Spring Modulith for modular architecture:

```
io.k48.fortyeightid
├── auth/                    # Authentication & Authorization
│   ├── internal/
│   │   ├── AuthService.java
│   │   ├── JwtTokenService.java
│   │   ├── RefreshTokenService.java
│   │   └── PasswordResetService.java
│   ├── ApiKey.java
│   ├── ApiKeyAuthFilter.java
│   ├── ApiKeyManagementPort.java
│   ├── EmailPort.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwksController.java
│   ├── LoginAttemptService.java
│   ├── PasswordPolicyService.java
│   └── PasswordResetPort.java
│
├── identity/                # User Identity Management
│   ├── internal/
│   │   ├── UserService.java
│   │   ├── UserQueryService.java
│   │   ├── MeController.java
│   │   └── UserRepository.java
│   ├── Role.java
│   ├── User.java
│   ├── UserQueryService.java
│   ├── UserRoleService.java
│   ├── UserStatus.java
│   ├── UserStatusService.java
│   ├── UserUpdateService.java
│   └── UserProvisioningPort.java
│
├── admin/                   # Administrative Operations
│   ├── internal/
│   │   ├── AdminUserService.java
│   │   ├── AdminApiKeyController.java
│   │   ├── AdminUserController.java
│   │   └── AdminAuditController.java
│   ├── CreateApiKeyRequest.java
│   ├── ApiKeyResponse.java
│   ├── AuditLogResponse.java
│   ├── ChangeRoleRequest.java
│   ├── ChangeStatusRequest.java
│   └── UpdateUserRequest.java
│
├── audit/                   # Audit Logging
│   ├── internal/
│   │   ├── AuditLog.java
│   │   └── AuditLogRepository.java
│   ├── AuditContext.java
│   ├── AuditContextAspect.java
│   ├── AuditEvent.java
│   └── AuditService.java
│
├── provisioning/            # CSV Import & Bulk Operations
│   ├── internal/
│   │   ├── CsvImportService.java
│   │   ├── CsvImportController.java
│   │   ├── CsvRow.java
│   │   └── CsvImportResult.java
│   └── package-info.java
│
├── shared/                  # Shared Components
│   ├── config/
│   │   ├── OpenApiConfig.java
│   │   ├── RateLimitConfig.java
│   │   ├── RateLimitFilter.java
│   │   ├── SecurityConfig.java
│   │   ├── CacheControlFilter.java
│   │   └── ProblemDetail*.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── *Exception.java
│
└── Application.java
```

## Module Dependencies

```
┌────────────────────────────────────────────────────────┐
│                      admin                              │
│  (depends on: auth, identity, audit)                   │
└────────────────────────────────────────────────────────┘
         │
┌────────┴────────┐
│                 │
▼                 ▼
┌────────────────────────────────┐  ┌──────────────────────────┐
│            auth                 │  │        identity          │
│  (depends on: audit, shared)   │  │  (depends on: audit)     │
└────────────────────────────────┘  └──────────────────────────┘
         │                                   │
         └──────────────┬────────────────────┘
                        │
                        ▼
              ┌─────────────────┐
              │      audit       │
              │  (depends on:    │
              │     shared)      │
              └─────────────────┘
                        │
                        ▼
              ┌─────────────────┐
              │     shared       │
              │  (no dependencies)│
              └─────────────────┘
```

## Key Design Decisions

### 1. API-First Design
- All functionality exposed via REST APIs
- OpenAPI documentation auto-generated
- Consistent error responses (RFC 7807)

### 2. Security by Default
- All endpoints secured by default
- Explicit permit-all configuration
- Method-level security with `@PreAuthorize`

### 3. Stateless Authentication
- JWT access tokens (RS256 signed)
- Refresh tokens stored in Redis
- No server-side session state

### 4. Modular Architecture
- Spring Modulith enforces module boundaries
- Each module has clear responsibilities
- Public APIs via Port interfaces

### 5. Audit Everything
- All authentication events logged
- Admin actions tracked
- Queryable audit log

## Data Flow Examples

### Login Flow

```
Client                    48ID                    PostgreSQL    Redis
  │                        │                         │           │
  │──POST /auth/login────▶│                         │           │
  │                        │──Find User─────────────▶│           │
  │                        │◀─User Data──────────────│           │
  │                        │──Validate Password──────▶│           │
  │                        │                         │           │
  │                        │──Generate JWT────────────▶│           │
  │                        │──Store Refresh Token───────────────▶│
  │◀─Access + Refresh─────│                         │           │
  │   Token                │                         │           │
```

### Token Refresh Flow

```
Client                    48ID                    PostgreSQL    Redis
  │                        │                         │           │
  │──POST /auth/refresh──▶│                         │           │
  │   (Refresh Token)      │                         │           │
  │                        │──Validate Token───────────────────▶│
  │                        │◀─Valid/Invalid────────────────────│
  │                        │                         │           │
  │                        │──Rotate Refresh Token────────────▶│
  │                        │──Generate New JWT────────▶│           │
  │◀─New Access +─────────│                         │           │
  │   Refresh Token        │                         │           │
```

### External App Token Verification

```
External App              48ID                    PostgreSQL    Redis
  │                        │                         │           │
  │──POST /auth/verify───▶│                         │           │
  │   (JWT + API Key)      │                         │           │
  │                        │──Validate API Key─────────────────▶│
  │                        │◀─Valid────────────────────────────│
  │                        │                         │           │
  │                        │──Validate JWT────────────▶│           │
  │                        │◀─Claims───────────────────│           │
  │◀─User Claims──────────│                         │           │
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/fortyeightid` |
| `DATABASE_USERNAME` | Database username | `fortyeightid` |
| `DATABASE_PASSWORD` | Database password | `fortyeightid` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_ISSUER` | JWT issuer URI | `http://localhost:8080` |
| `JWT_ACCESS_TOKEN_EXPIRY` | Access token TTL (seconds) | `900` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh token TTL (seconds) | `86400` (1 day) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:3000` |
| `MAIL_HOST` | SMTP host | `localhost` |
| `MAIL_PORT` | SMTP port | `1025` |

### Application Properties

See `src/main/resources/application.properties` for full configuration.

## Next Steps

- [System Architecture](../architecture/system.md) - Detailed system design
- [Database Schema](../architecture/database.md) - Database structure
- [Security Architecture](../architecture/security.md) - Security design
