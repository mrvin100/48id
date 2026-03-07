# Contributing to 48ID

## Prerequisites
- Java 21
- Docker & Docker Compose

## Quick Start

```bash
# Start infrastructure
docker compose up -d

# Run the application
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests
./gradlew test

# Build
./gradlew build

# Verify module structure
./gradlew test --tests ApplicationModularityTests
```

## Project Structure

The project follows Spring Modulith conventions:
- `identity/` — User and organization management
- `auth/` — Authentication and authorization
- `admin/` — Admin dashboard operations
- `provisioning/` — Automated user provisioning (CSV/SCIM)
- `audit/` — Audit logging
- `shared/` — Shared DTOs, configs, exceptions
