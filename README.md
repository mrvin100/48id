# 48ID

**Centralized Identity and Authentication Platform for the K48 Ecosystem**

[![Build Status](https://github.com/mrvin100/48id/actions/workflows/ci.yml/badge.svg)](https://github.com/mrvin100/48id/actions)
[![License](https://img.shields.io/badge/license-K48-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

## Overview

48ID is the unified identity backbone of the K48 ecosystem — a standalone, API-first Identity Provider (IdP) that eliminates the need for every student-built application to implement its own authentication system.

**Key Benefits:**
- ✅ Single Sign-On (SSO) across all K48 platforms
- ✅ Enterprise-grade security (JWT, rate limiting, audit logging)
- ✅ Developer-friendly integration (< 1 day using Swagger)
- ✅ Centralized user management and audit trail

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Git

### 1. Clone Repository
```bash
git clone https://github.com/mrvin100/48id.git
cd 48id
```

### 2. Start Infrastructure
```bash
docker compose up -d
```

### 3. Run Application
```bash
./gradlew bootRun
```

### 4. Access Swagger UI
Open http://localhost:8080/api/v1/docs

## Key Features (MVP)

### Authentication
- Matricule-based login for K48 students and administrators
- JWT access tokens (15 min, RS256 signed)
- Refresh tokens (7 days, Redis-backed)
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
- OpenAPI/Swagger documentation
- Consistent error responses (RFC 7807)
- Integration guides and examples

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

## Architecture

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

See [Architecture Documentation](docs/overview/architecture.md) for details.

## Documentation

Comprehensive documentation is available in the [`docs/`](docs/) directory:

- **[Overview](docs/README.md)** - Documentation index
- **[Quick Start](docs/overview/quickstart.md)** - Get started in minutes
- **[Architecture](docs/overview/architecture.md)** - System design
- **[API Reference](docs/api/overview.md)** - Complete API documentation
- **[Integration Guide](docs/integration/getting-started.md)** - Integrate your app
- **[Admin Operations](docs/admin/overview.md)** - Admin user guide
- **[Deployment](docs/deployment/overview.md)** - Deploy to production

**Swagger UI:** http://localhost:8080/api/v1/docs

## Integration Example

### 1. Request API Key
Contact K48 administration to obtain an API key.

### 2. Verify User Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-token \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"token": "user-jwt-token"}'
```

### 3. Get User Identity
```bash
curl -X GET http://localhost:8080/api/v1/users/{user-id}/identity \
  -H "X-API-Key: your-api-key"
```

See [Integration Guide](docs/integration/getting-started.md) for complete examples.

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
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:3000` |

See [Configuration Guide](docs/deployment/configuration.md) for full configuration options.

## Testing

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test
```bash
./gradlew test --tests "io.k48.fortyeightid.auth.internal.AuthServiceTest"
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Workflow
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m "Add amazing feature"`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

48ID is licensed under the K48 License. See [LICENSE](LICENSE) for details.

## Support

- **Documentation:** [docs/](docs/)
- **Swagger UI:** http://localhost:8080/api/v1/docs
- **Email:** support@k48.io

## Acknowledgments

48ID is part of the K48 (KFOKAM48) ecosystem, providing centralized identity management for all K48 platforms.

---

**Built with ❤️ for the K48 Ecosystem**
