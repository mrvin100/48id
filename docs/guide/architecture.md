# Architecture

## System Overview

48ID is the centralized identity provider for the K48 ecosystem, built with **Spring Boot 3** and **Spring Modulith** for clean, maintainable architecture.

```mermaid
graph TB
    subgraph "K48 Ecosystem Apps"
        Hub[48Hub<br/>Alumni Platform]
        LP[LP48<br/>Project Showcase]
        Admin[Admin Portal]
    end

    subgraph "48ID - Identity Provider"
        direction TB
        API[REST API Layer<br/>Spring Security]
        
        subgraph "Domain Modules"
            Auth[auth<br/>Authentication & JWT]
            Identity[identity<br/>User Management]
            Provisioning[provisioning<br/>CSV Import]
            AdminMod[admin<br/>Admin Operations]
            Audit[audit<br/>Audit Logging]
        end
        
        Shared[shared<br/>Security & Infrastructure]
    end

    subgraph "Infrastructure"
        PG[(PostgreSQL<br/>Primary Store)]
        Redis[(Redis<br/>Cache & Rate Limit)]
        SMTP[SMTP Server<br/>Transactional Email]
    end

    Hub -.->|JWT Bearer| API
    LP -.->|JWT Bearer| API
    Admin -.->|JWT Bearer| API
    
    API --> Auth
    API --> Identity
    API --> Provisioning
    API --> AdminMod
    
    Auth --> Audit
    Identity --> Audit
    Provisioning --> Audit
    AdminMod --> Audit
    
    Auth --> PG
    Identity --> PG
    Provisioning --> PG
    AdminMod --> PG
    Audit --> PG
    
    API --> Redis
    Auth --> SMTP
    API --> Shared
```

## Spring Modulith Architecture

48ID uses **Spring Modulith** to enforce module boundaries at compile-time and runtime.

### Module Structure

```
io.k48.fortyeightid/
├── auth/                    # Authentication module
│   ├── internal/           # Private implementation
│   ├── ports/              # Public interfaces
│   └── package-info.java   # Module metadata
├── identity/                # Identity management module
│   ├── internal/
│   └── package-info.java
├── admin/                   # Admin operations module
│   ├── internal/
│   └── package-info.java
├── provisioning/            # User provisioning module
│   ├── internal/
│   └── package-info.java
├── audit/                   # Audit logging module
│   ├── internal/
│   └── package-info.java
└── shared/                  # Shared infrastructure
    ├── config/
    ├── exception/
    └── package-info.java
```

### Module Responsibilities

| Module | Responsibility | Public Interfaces |
|--------|----------------|-------------------|
| **auth** | Authentication, JWT, password flows, activation, API keys | `PasswordResetPort`, `EmailPort`, `ApiKeyManagementPort` |
| **identity** | User entity, profiles, roles, status management | `UserQueryService`, `UserProvisioningPort`, `UserRoleService` |
| **admin** | Privileged user operations, API key admin, audit access | Admin controllers |
| **provisioning** | CSV import, bulk user creation with activation | Provisioning controllers |
| **audit** | Audit event persistence and querying | `AuditService` |
| **shared** | Security config, rate limiting, error handling | Global exception handler, security filters |

### Module Communication

Modules communicate through **public ports** (interfaces) to maintain loose coupling:

```mermaid
graph LR
    Prov[provisioning] -->|UserProvisioningPort| Identity[identity]
    Prov -->|PasswordResetPort| Auth[auth]
    Admin -->|UserQueryService| Identity
    Admin -->|ApiKeyManagementPort| Auth
    Auth -->|UserQueryService| Identity
    Auth -->|AuditService| Audit[audit]
```

**Rules:**
- ✅ Modules can call public ports from other modules
- ❌ Modules cannot access `internal/` packages of other modules
- ❌ No circular dependencies between modules
- ✅ Validated at build time by `ApplicationModularityTests`

## Authentication Architecture

### JWT Token Flow

```mermaid
sequenceDiagram
    participant User
    participant App as K48 App
    participant API as 48ID API
    participant DB as PostgreSQL
    
    User->>App: Enter credentials
    App->>API: POST /auth/login
    API->>DB: Validate user
    DB-->>API: User record
    API->>API: Generate JWT (RS256)
    API->>API: Create refresh token
    API->>DB: Store refresh token
    API-->>App: access_token + refresh_token
    App->>App: Store tokens
    App->>API: GET /me (Bearer token)
    API->>API: Validate JWT signature
    API->>DB: Check user status
    API-->>App: User profile
```

### Token Types

| Token | Lifetime | Purpose | Storage |
|-------|----------|---------|---------|
| **Access Token** | 15 minutes | API authentication | Memory (not localStorage) |
| **Refresh Token** | 86400 seconds (1 day) | Get new access token | HttpOnly cookie recommended |
| **Activation Token** | 24 hours | Account activation | Email link only |
| **Reset Token** | 1 hour | Password reset | Email link only |
| **API Key** | No expiration | Server-to-server auth | Environment variables |

### Security Features

- **RS256 signatures** — Asymmetric JWT signing
- **Refresh token rotation** — New refresh token on each use
- **Token revocation** — Refresh tokens can be revoked
- **Rate limiting** — 5 login attempts per 15 min per matricule
- **Password policies** — Minimum length, complexity requirements
- **Audit logging** — All auth events logged

## Database Architecture

### Entity Model

```mermaid
erDiagram
    users ||--o{ user_roles : has
    roles ||--o{ user_roles : assigned
    users ||--o{ refresh_tokens : owns
    users ||--o{ password_reset_tokens : owns
    users ||--o{ audit_log : generates
    users }o--|| users : created_by
    api_keys }o--|| users : created_by

    users {
        uuid id PK
        string matricule UK
        string email UK
        string name
        string password_hash
        string phone
        string batch
        string specialization
        enum status
        boolean profile_completed
        boolean requires_password_change
        timestamp created_at
        timestamp updated_at
    }

    roles {
        uuid id PK
        string name UK
    }

    refresh_tokens {
        uuid id PK
        uuid user_id FK
        string token_hash UK
        timestamp expires_at
        timestamp created_at
    }

    password_reset_tokens {
        uuid id PK
        uuid user_id FK
        string token UK
        enum purpose
        timestamp expires_at
        boolean used
    }

    api_keys {
        uuid id PK
        string app_name
        string key_hash UK
        string description
        boolean active
        timestamp last_used_at
        uuid created_by FK
    }

    audit_log {
        uuid id PK
        uuid user_id FK
        string action
        jsonb metadata
        string ip_address
        string user_agent
        timestamp timestamp
    }
```

### Schema Evolution

Database schema is managed with **Flyway** versioned migrations:

```
src/main/resources/db/migration/
├── V1__baseline.sql
├── V2__api_keys.sql
├── V3__api_keys_add_description_and_last_used.sql
├── V4__add_requires_password_change_column.sql
├── V5__add_created_by_to_api_keys.sql
├── V6__add_user_agent_to_audit_log.sql
└── V7__add_purpose_to_password_reset_tokens.sql
```

Migrations run automatically on application startup.

## Infrastructure Components

### PostgreSQL
- **Purpose:** Primary data store
- **Version:** 17+
- **Features used:** UUIDs, JSONB, indexes, constraints

### Redis
- **Purpose:** Rate limiting, session support
- **Version:** 7.4+
- **Use cases:** Bucket4j rate limit state

### SMTP
- **Purpose:** Transactional emails
- **Use cases:** Account activation, password reset

### Springdoc OpenAPI
- **Purpose:** Interactive API documentation
- **Access:** `/api/v1/docs` (Swagger UI)

## Deployment Architecture

### Recommended Production Topology

```mermaid
graph TB
    subgraph "External"
        Client[K48 Apps]
    end
    
    subgraph "Network Edge"
        LB[Load Balancer<br/>nginx/AWS ALB]
    end
    
    subgraph "Application Tier"
        App1[48ID Instance 1]
        App2[48ID Instance 2]
        App3[48ID Instance N]
    end
    
    subgraph "Data Tier"
        PG[(PostgreSQL<br/>Primary + Replica)]
        Redis[(Redis<br/>Cluster)]
    end
    
    subgraph "External Services"
        SMTP[SMTP Provider<br/>SendGrid/SES]
        Secrets[Secret Manager<br/>Vault/AWS Secrets]
    end
    
    Client -->|HTTPS| LB
    LB --> App1
    LB --> App2
    LB --> App3
    
    App1 --> PG
    App2 --> PG
    App3 --> PG
    
    App1 --> Redis
    App2 --> Redis
    App3 --> Redis
    
    App1 --> SMTP
    App2 --> SMTP
    App3 --> SMTP
    
    App1 -.->|Read secrets| Secrets
    App2 -.->|Read secrets| Secrets
    App3 -.->|Read secrets| Secrets
```

### Horizontal Scaling

48ID is **stateless** and can scale horizontally:
- ✅ Multiple instances behind a load balancer
- ✅ Shared PostgreSQL and Redis
- ✅ No in-memory session state
- ✅ JWTs are self-contained and validated independently

## Security Model

### Defense in Depth

```mermaid
graph TD
    A[HTTPS/TLS] --> B[Rate Limiting]
    B --> C[JWT Validation]
    C --> D[Role-Based Access Control]
    D --> E[Audit Logging]
    
    A1[Network Edge] -.-> A
    B1[Bucket4j + Redis] -.-> B
    C1[RS256 Signature] -.-> C
    D1[@PreAuthorize] -.-> D
    E1[PostgreSQL] -.-> E
```

**Layers:**
1. **Transport:** HTTPS/TLS in production
2. **Rate limiting:** Per-IP and per-user limits
3. **Authentication:** JWT signature validation
4. **Authorization:** Role-based access control
5. **Audit:** Complete event trail

### Threat Model

| Threat | Mitigation |
|--------|------------|
| Credential stuffing | Rate limiting on `/auth/login` |
| Token theft | Short-lived access tokens, refresh token rotation |
| SQL injection | JPA parameterized queries |
| XSS | JSON responses, no HTML rendering |
| CSRF | Stateless API, no cookies for auth |
| Enumeration | Generic error messages, email enumeration protection |

## Performance Considerations

### Caching Strategy
- ✅ JWT validation is signature-based (no DB lookup per request)
- ✅ JWKS cached by clients
- ⚠️ User lookups hit database (consider caching for high-traffic apps)

### Database Optimization
- ✅ Indexed columns: `matricule`, `email`, `token`
- ✅ Connection pooling with HikariCP
- ✅ Prepared statement caching

### Monitoring Points
- Request rate and latency
- Database connection pool utilization
- Redis connection health
- JWT validation failures
- Rate limit hits
- Audit log volume

## Testing Strategy

The architecture is validated by different test layers:

| Test Type | Coverage | Tools |
|-----------|----------|-------|
| **Unit Tests** | Business logic, services | JUnit 5, Mockito |
| **Module Tests** | Module boundaries | Spring Modulith |
| **Integration Tests** | End-to-end flows | Spring Boot Test, Testcontainers |
| **Contract Tests** | API contracts | Spring MockMvc |

**Key test:** `ApplicationModularityTests` enforces that module boundaries are respected.

## Next Steps

- **[Authentication Guide](authentication.md)** — Deep dive into auth flows
- **[Integration Guide](integration.md)** — How to integrate your app
- **[Deployment Guide](deployment.md)** — Deploy to production
- **[API Reference](../api/overview.md)** — Complete API documentation
