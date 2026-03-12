# Testing guide

## Test layers in the repository

The MVP repository contains:

- unit and service tests for auth, identity, admin, provisioning, and shared concerns
- controller tests for key HTTP surfaces
- Modulith verification tests
- Spring Boot application tests
- Testcontainers-based database testing support

## Important test types

### Unit and service tests

Used for domain and service logic such as:

- password policy validation
- password reset and activation flows
- API key behavior
- user administration logic
- CSV validation and import behavior

### Controller tests

Used for HTTP contract validation on controllers and filters.

### Modulith tests

`ApplicationModularityTests` validates Spring Modulith boundaries and helps prevent accidental architectural erosion.

### Integration support

`application-test.properties` uses Testcontainers-backed PostgreSQL. The repository also includes Redis test support.

## Running tests

All tests:

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

## CI coverage

The GitHub Actions workflow runs a full Gradle build with PostgreSQL and Redis service containers.

## Documentation change policy

When behavior changes:

- update the related tests
- update the related docs
- verify that both pass together
