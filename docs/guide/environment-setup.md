# Environment Setup Guide

This guide explains how to configure 48ID for different environments using our unified environment variable strategy.

## Quick Start

1. **Copy the environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Choose your environment profile** by setting `SPRING_PROFILES_ACTIVE` in your `.env` file:
   - `dev` - Development (default)
   - `test` - Testing/CI
   - `prod` - Production

3. **Start required services:**
   ```bash
   # For development
   docker compose up -d db redis mailpit
   
   # Run the application
   ./gradlew bootRun
   ```

## Environment Profiles

### Development (`dev`)
- **Purpose**: Local development with debugging tools
- **Features**:
  - Detailed SQL logging
  - Swagger UI enabled at `/api/v1/docs`
  - Debug-level logging for application code
  - Uses Docker Compose services (PostgreSQL on port 5433, Redis, Mailpit)

### Testing (`test`)
- **Purpose**: Automated testing and CI/CD
- **Features**:
  - Testcontainers for isolated database testing
  - Mock email services
  - Faster startup (Swagger disabled)
  - Separate Redis port to avoid conflicts

### Production (`prod`)
- **Purpose**: Production deployment
- **Features**:
  - Swagger UI disabled for security
  - Minimal logging (WARN level)
  - Compression enabled
  - Restricted actuator endpoints

## Environment Variables

All configuration is done through environment variables with sensible defaults. The `.env.example` file contains all available options with documentation.

### Key Variables

| Variable | Description | Dev Default | Prod Example |
|----------|-------------|-------------|--------------|
| `SPRING_PROFILES_ACTIVE` | Environment profile | `dev` | `prod` |
| `DATABASE_URL` | PostgreSQL connection | `localhost:5433` | `prod-db.example.com:5432` |
| `REDIS_HOST` | Redis server | `localhost` | `prod-redis.example.com` |
| `MAIL_HOST` | SMTP server | `localhost` (Mailpit) | `smtp.sendgrid.net` |
| `CORS_ALLOWED_ORIGINS` | Frontend URLs | `http://localhost:3000` | `https://app.yourdomain.com` |
| `SPRINGDOC_SWAGGER_ENABLED` | API docs | `true` | `false` |

## Docker Compose Services

The `docker-compose.yml` provides development services:

- **`db`**: PostgreSQL 17 database (port 5433)
- **`redis`**: Redis 7.4 for sessions and rate limiting
- **`mailpit`**: Email testing tool with web UI at http://localhost:8025

### Service Commands

```bash
# Start all services
docker compose up -d

# Start specific services
docker compose up -d db redis mailpit

# View logs
docker compose logs -f db

# Stop services
docker compose down
```

## Environment-Specific Setup

### Development Setup

1. Copy `.env.example` to `.env`
2. Ensure `SPRING_PROFILES_ACTIVE=dev`
3. Start Docker services: `docker compose up -d db redis mailpit`
4. Run application: `./gradlew bootRun`
5. Access Swagger UI: http://localhost:8080/api/v1/docs
6. View emails: http://localhost:8025

### Production Setup

1. Create `.env.prod` with production values:
   ```bash
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=jdbc:postgresql://your-prod-db:5432/fortyeightid
   DATABASE_USERNAME=prod_user
   DATABASE_PASSWORD=secure_password
   REDIS_HOST=your-prod-redis
   REDIS_PASSWORD=redis_password
   MAIL_HOST=smtp.sendgrid.net
   MAIL_PORT=587
   MAIL_USERNAME=apikey
   MAIL_PASSWORD=your_sendgrid_api_key
   MAIL_SMTP_AUTH=true
   MAIL_SMTP_STARTTLS=true
   JWT_ISSUER=https://id.yourdomain.com
   CORS_ALLOWED_ORIGINS=https://app.yourdomain.com
   SPRINGDOC_SWAGGER_ENABLED=false
   ```

2. Generate production RSA keys (see Security section)
3. Deploy using your preferred method (Docker, JAR, etc.)

### Testing Setup

For CI/CD, create `.env.test`:
```bash
SPRING_PROFILES_ACTIVE=test
# Most values will use application-test.properties defaults
```

## Security Considerations

### JWT Keys
- **Development**: Uses provided keys in `src/main/resources/keys/`
- **Production**: Generate new RSA key pair and store securely

### Database
- **Development**: Uses Docker with default credentials
- **Production**: Use managed database service with strong credentials

### Email
- **Development**: Uses Mailpit (no real emails sent)
- **Production**: Use reputable SMTP service (SendGrid, AWS SES, etc.)

## Troubleshooting

### Common Issues

1. **Port conflicts**: Change `DATABASE_HOST_PORT` if 5433 is in use
2. **Docker not running**: Ensure Docker is started before `docker compose up`
3. **Permission errors**: Check file permissions on `.env` file
4. **Database connection**: Verify PostgreSQL is running and accessible

### Logs

View application logs with different detail levels:
- **Development**: DEBUG level with SQL logging
- **Production**: INFO level, minimal output

### Health Checks

Access health endpoints:
- Development: http://localhost:8080/actuator/health
- Production: Limited to basic health info

## Migration from Old Setup

If you have existing environment files:

1. **Backup** your current `.env` and profile-specific files
2. **Copy** `.env.example` to `.env`
3. **Migrate** your custom values to the new `.env` file
4. **Test** with `./gradlew bootRun`
5. **Remove** old environment files once confirmed working

The new setup consolidates all environment configuration into a single `.env` file with profile-based overrides, making it easier to manage and deploy across different environments.

## Understanding Spring Boot Profile Loading

### How Property Loading Works

Spring Boot uses a **hierarchical property loading system** where properties are loaded in a specific order, and **later sources override earlier ones**. This is crucial to understand for effective configuration management.

#### Property Loading Priority (Highest to Lowest)

```mermaid
graph TD
    A["Environment Variables<br/>(.env file)"] --> B["Profile Properties<br/>(application-profile.properties)"]
    B --> C["Base Properties<br/>(application.properties)"]
    
    A -.->|"HIGHEST PRIORITY<br/>Overrides everything"| D["Final Configuration"]
    B -.->|"MEDIUM PRIORITY<br/>Overrides base"| D
    C -.->|"LOWEST PRIORITY<br/>Default values"| D
    
    style A fill:#ff6b6b,stroke:#d63031,color:#fff
    style B fill:#fdcb6e,stroke:#e17055,color:#000
    style C fill:#74b9ff,stroke:#0984e3,color:#fff
    style D fill:#00b894,stroke:#00a085,color:#fff
```

#### Profile Loading Sequence

```mermaid
sequenceDiagram
    participant App as Spring Boot App
    participant Env as Environment Variables
    participant Base as application.properties
    participant Profile as application-profile.properties
    participant Config as Final Configuration
    
    Note over App: Application starts with SPRING_PROFILES_ACTIVE=dev
    
    App->>Base: 1. Load base properties
    Base-->>App: logging.level.root=INFO, springdoc.swagger-ui.enabled=true
    
    App->>Profile: 2. Load application-dev.properties
    Profile-->>App: logging.level.root=DEBUG, spring.jpa.show-sql=true
    
    App->>Env: 3. Resolve environment variables
    Env-->>App: DATABASE_URL=jdbc:postgresql://localhost:5433/...
    
    App->>Config: 4. Merge all properties
    Note over Config: Final result: logging.level.root=DEBUG (from profile), springdoc.swagger-ui.enabled=true (from base), spring.jpa.show-sql=true (from profile), DATABASE_URL=... (from env)
```

### Property Resolution Examples

#### Example 1: Development Profile (`SPRING_PROFILES_ACTIVE=dev`)

```mermaid
graph LR
    subgraph "1. Base Properties"
        A1["logging.level.io.k48=INFO"]
        A2["spring.jpa.show-sql=false"]
        A3["springdoc.swagger-ui.enabled=true"]
    end
    
    subgraph "2. Dev Profile Override"
        B1["logging.level.io.k48=DEBUG"]
        B2["spring.jpa.show-sql=true"]
        B3["logging.level.hibernate.SQL=DEBUG"]
    end
    
    subgraph "3. Environment Variables"
        C1["DATABASE_URL=jdbc:postgresql://localhost:5433/..."]
        C2["SPRING_PROFILES_ACTIVE=dev"]
    end
    
    subgraph "4. Final Configuration"
        D1["logging.level.io.k48=DEBUG (FINAL)"]
        D2["spring.jpa.show-sql=true (FINAL)"]
        D3["springdoc.swagger-ui.enabled=true (FINAL)"]
        D4["logging.level.hibernate.SQL=DEBUG (FINAL)"]
        D5["DATABASE_URL=jdbc:postgresql://localhost:5433/... (FINAL)"]
    end
    
    A1 -.->|overridden| B1
    A2 -.->|overridden| B2
    A3 -.->|inherited| D3
    B1 --> D1
    B2 --> D2
    B3 --> D4
    C1 --> D5
    
    style B1 fill:#ff6b6b,color:#fff
    style B2 fill:#ff6b6b,color:#fff
    style D1 fill:#00b894,color:#fff
    style D2 fill:#00b894,color:#fff
    style D3 fill:#00b894,color:#fff
    style D4 fill:#00b894,color:#fff
    style D5 fill:#00b894,color:#fff
```

#### Example 2: Production Profile (`SPRING_PROFILES_ACTIVE=prod`)

```mermaid
graph LR
    subgraph "1. Base Properties"
        A1["logging.level.root=INFO"]
        A2["springdoc.swagger-ui.enabled=true"]
        A3["management.endpoints.include=health,info,metrics"]
    end
    
    subgraph "2. Prod Profile Override"
        B1["logging.level.root=WARN"]
        B2["springdoc.swagger-ui.enabled=false"]
        B3["management.endpoints.include=health,info"]
        B4["server.compression.enabled=true"]
    end
    
    subgraph "3. Environment Variables"
        C1["DATABASE_URL=jdbc:postgresql://prod-db:5432/..."]
        C2["SPRINGDOC_SWAGGER_ENABLED=false"]
    end
    
    subgraph "4. Final Configuration"
        D1["logging.level.root=WARN (FINAL)"]
        D2["springdoc.swagger-ui.enabled=false (FINAL)"]
        D3["management.endpoints.include=health,info (FINAL)"]
        D4["server.compression.enabled=true (FINAL)"]
        D5["DATABASE_URL=jdbc:postgresql://prod-db:5432/... (FINAL)"]
    end
    
    A1 -.->|overridden| B1
    A2 -.->|overridden| B2
    A3 -.->|overridden| B3
    B1 --> D1
    B2 --> D2
    B3 --> D3
    B4 --> D4
    C1 --> D5
    
    style B1 fill:#ff6b6b,color:#fff
    style B2 fill:#ff6b6b,color:#fff
    style B3 fill:#ff6b6b,color:#fff
    style D1 fill:#00b894,color:#fff
    style D2 fill:#00b894,color:#fff
    style D3 fill:#00b894,color:#fff
    style D4 fill:#00b894,color:#fff
    style D5 fill:#00b894,color:#fff
```

### Key Principles

#### 1. Properties are MERGED, not REPLACED

When you activate a profile, Spring Boot doesn't discard `application.properties`. Instead:

- **Base properties** provide defaults for all environments
- **Profile properties** override only what needs to be different
- **Environment variables** provide deployment-specific values

#### 2. Environment Variables Have Ultimate Priority

```properties
# application.properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/fortyeightid}
#                      ↑              ↑
#                   env var        fallback default

# .env file  
DATABASE_URL=jdbc:postgresql://localhost:5433/fortyeightid

# Result: Uses 5433 (from env var), not 5432 (fallback)
```

#### 3. Profile Auto-Detection

Spring Boot automatically loads the correct profile files:

```bash
SPRING_PROFILES_ACTIVE=dev
# Automatically loads:
# 1. application.properties
# 2. application-dev.properties  ← detected automatically
```

### Practical Configuration Strategy

#### File Organization

```
src/main/resources/
├── application.properties          # Base configuration (common to all environments)
├── application-dev.properties      # Development overrides
├── application-test.properties     # Testing overrides
└── application-prod.properties     # Production overrides
```

#### What Goes Where

**`application.properties` (Base)**:
- Database connection templates with environment variable placeholders
- Common Spring Boot settings
- Default logging levels
- Feature flags with environment variable overrides

**`application-dev.properties` (Development)**:
- Debug logging levels
- SQL query logging
- Development tool enablement (Swagger, etc.)

**`application-prod.properties` (Production)**:
- Security hardening
- Performance optimizations
- Minimal logging
- Disabled development features

**Environment Variables (`.env`)**:
- Database credentials and URLs
- External service configurations
- Deployment-specific values
- Profile selection

### Real-World Example

Let's trace a complete property resolution:

**Property**: `springdoc.swagger-ui.enabled`

```mermaid
flowchart TD
    Start([Application Starts]) --> CheckProfile{Check SPRING_PROFILES_ACTIVE}
    
    CheckProfile -->|dev| LoadDev["Load application-dev.properties"]
    CheckProfile -->|prod| LoadProd["Load application-prod.properties"]
    CheckProfile -->|test| LoadTest["Load application-test.properties"]
    
    LoadDev --> DevResult["springdoc.swagger-ui.enabled=true<br/>From base properties<br/>Not overridden in dev profile"]
    LoadProd --> ProdResult["springdoc.swagger-ui.enabled=false<br/>Overridden in prod profile<br/>Security: disabled in production"]
    LoadTest --> TestResult["springdoc.swagger-ui.enabled=false<br/>Overridden in test profile<br/>Performance: disabled for faster tests"]
    
    DevResult --> EnvCheck1{"Environment Variable<br/>SPRINGDOC_SWAGGER_ENABLED?"}
    ProdResult --> EnvCheck2{"Environment Variable<br/>SPRINGDOC_SWAGGER_ENABLED?"}
    TestResult --> EnvCheck3{"Environment Variable<br/>SPRINGDOC_SWAGGER_ENABLED?"}
    
    EnvCheck1 -->|Set| EnvOverride1["Use Environment Value"]
    EnvCheck1 -->|Not Set| FinalDev["Final: true"]
    
    EnvCheck2 -->|Set| EnvOverride2["Use Environment Value"]
    EnvCheck2 -->|Not Set| FinalProd["Final: false"]
    
    EnvCheck3 -->|Set| EnvOverride3["Use Environment Value"]
    EnvCheck3 -->|Not Set| FinalTest["Final: false"]
    
    EnvOverride1 --> FinalEnv1["Final: Environment Value"]
    EnvOverride2 --> FinalEnv2["Final: Environment Value"]
    EnvOverride3 --> FinalEnv3["Final: Environment Value"]
    
    style Start fill:#74b9ff,color:#fff
    style FinalDev fill:#00b894,color:#fff
    style FinalProd fill:#00b894,color:#fff
    style FinalTest fill:#00b894,color:#fff
    style FinalEnv1 fill:#ff6b6b,color:#fff
    style FinalEnv2 fill:#ff6b6b,color:#fff
    style FinalEnv3 fill:#ff6b6b,color:#fff
```

### Best Practices

1. **Keep base properties environment-agnostic**
2. **Use environment variables for sensitive data**
3. **Profile properties should only contain overrides**
4. **Document why each override exists**
5. **Test configuration in all target environments**

### Debugging Configuration

To see which properties are loaded and their sources:

```bash
# Enable configuration debugging
java -jar app.jar --debug --trace

# Or add to application.properties
logging.level.org.springframework.boot.context.config=DEBUG
```

This will show you exactly which files are loaded and how properties are resolved.

## Quick Reference

### Profile Activation

| Method | Example | Use Case |
|--------|---------|----------|
| Environment Variable | `SPRING_PROFILES_ACTIVE=dev` | Most common, set in `.env` |
| Command Line | `java -jar app.jar --spring.profiles.active=prod` | Override for specific runs |
| IDE Configuration | VM Options: `-Dspring.profiles.active=test` | Development/debugging |

### Property Priority (Highest to Lowest)

1. **Environment Variables** (`.env` file)
2. **Profile Properties** (`application-{profile}.properties`)
3. **Base Properties** (`application.properties`)

### Common Patterns

```properties
# Template pattern with fallback
property.name=${ENV_VAR_NAME:default-value}

# Profile-specific override
# application.properties:     logging.level.root=INFO
# application-dev.properties: logging.level.root=DEBUG

# Environment-only configuration
# .env: DATABASE_PASSWORD=secret123
# application.properties: spring.datasource.password=${DATABASE_PASSWORD}
```

### Profile File Loading

```
SPRING_PROFILES_ACTIVE=dev
├── application.properties          ✓ Always loaded
├── application-dev.properties      ✓ Loaded (profile match)
├── application-prod.properties     ✗ Not loaded
└── application-test.properties     ✗ Not loaded
```