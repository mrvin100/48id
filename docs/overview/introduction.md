# Introduction

48ID is the centralized identity and authentication platform for the K48 ecosystem. It provides a single source of truth for user identity, authentication, and authorization across all K48 applications.

## Purpose

Without 48ID, every K48 application would need to implement its own authentication system, leading to:
- Inconsistent security practices
- Duplicated effort
- Fragmented user experiences
- Maintenance overhead

48ID solves this by providing:
- **Centralized Authentication**: Single login for all K48 applications
- **Consistent Security**: Enterprise-grade security across the ecosystem
- **Developer Acceleration**: Integrate authentication in hours, not days
- **Unified Identity**: Single user profile across all platforms

## Key Features (MVP)

### Authentication
- Matricule-based login for K48 students and administrators
- JWT access tokens (15-minute expiry, RS256 signed)
- Refresh tokens (7-day expiry, stored in Redis)
- Password reset via email

### Authorization
- Role-based access control (ADMIN, STUDENT)
- API key authentication for external applications
- Method-level security enforcement

### User Management
- CSV bulk import for student provisioning
- User profile management
- Account status management (ACTIVE, SUSPENDED, PENDING_ACTIVATION)
- Profile completion tracking

### Security
- Rate limiting on authentication endpoints
- Account lockout after failed attempts
- Password policy enforcement
- Comprehensive audit logging

### Developer Experience
- OpenAPI/Swagger documentation at `/api/v1/docs`
- Consistent error responses (RFC 7807 ProblemDetail)
- Integration guides and examples

## Architecture Overview

48ID is built on Spring Boot 3 with Spring Modulith architecture:

```
┌─────────────────────────────────────────────────────────┐
│                    48ID Platform                         │
├─────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │   Auth   │  │ Identity │  │  Admin   │  │  Audit  │ │
│  │  Module  │  │  Module  │  │  Module  │  │ Module  │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Shared Module (Common Services)         │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  PostgreSQL (Users, Roles, Audit)  │  Redis (Tokens)   │
└─────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Backend Framework | Spring Boot 3 |
| Security | Spring Security 6 |
| Architecture | Spring Modulith |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| JWT | Nimbus JOSE + JWT |
| Rate Limiting | Bucket4j |
| Documentation | SpringDoc OpenAPI |
| Build Tool | Gradle |
| Containerization | Docker |

## Getting Started

### Prerequisites
- Java 21+
- Docker and Docker Compose
- Gradle 8+

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/mrvin100/48id.git
   cd 48id
   ```

2. **Start infrastructure**
   ```bash
   docker compose up -d
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access Swagger UI**
   - Open http://localhost:8080/api/v1/docs

### First Integration

1. **Request API Key**: Contact K48 administration
2. **Test Authentication**: Use Swagger UI to test endpoints
3. **Integrate**: Follow the [Integration Guide](../INTEGRATION_GUIDE.md)

## Documentation Navigation

- **New to 48ID?** Start with [Quick Start](overview/quickstart.md)
- **Integrating an application?** See [Integration Guides](integration/getting-started.md)
- **Admin user?** See [Admin Operations](admin/overview.md)
- **Deploying?** See [Deployment Guide](deployment/overview.md)

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

## License

48ID is licensed under the K48 License. See [LICENSE](../LICENSE) for details.
