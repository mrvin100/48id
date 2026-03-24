# Contributing to 48ID

Thank you for your interest in contributing to 48ID! 🎉

This document provides guidelines for contributing to the project.

## Quick Start for Development

### Prerequisites

- **Java 21+** ([Download](https://adoptium.net/))
- **Docker & Docker Compose** ([Download](https://www.docker.com/))
- **Git**
- **IDE** (IntelliJ IDEA recommended)

### Setup Development Environment

```bash
# 1. Clone the repository
git clone https://github.com/mrvin100/48id.git
cd 48id

# 2. Start dependencies
docker compose up -d

# 3. Run the application
./gradlew bootRun

# Windows:
.\gradlew.bat bootRun

# 4. Run tests
./gradlew test

# 5. Build
./gradlew build
```

The application will start on http://localhost:8080

## Development Workflow

We follow a standard Git workflow for contributions:

### 1. Create a Feature Branch

```bash
# Ensure you're on main and up to date
git checkout main
git pull origin main

# Create a feature branch
git checkout -b feature/48ID-<STORY-ID>-<short-description>

# Example:
git checkout -b feature/48ID-E07-01-oauth-support
```

**Branch naming convention:**
- `feature/` — New features
- `fix/` — Bug fixes
- `docs/` — Documentation updates
- `refactor/` — Code refactoring

### 2. Make Your Changes

- Write clean, readable code
- Follow existing code style
- Add tests for new functionality
- Update documentation as needed

### 3. Test Your Changes

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "ClassName"

# Verify module boundaries
./gradlew test --tests ApplicationModularityTests

# Check code coverage
./gradlew jacocoTestReport
```

### 4. Commit Your Changes

```bash
git add .
git commit -m "feat: add OAuth support (48ID-E07-01)"
```

**Commit message format:**
```text
<type>: <description> (<story-id>)

Examples:
feat: add OAuth support (48ID-E07-01)
fix: resolve token expiration bug (48ID-E07-02)
docs: update API documentation
refactor: simplify auth service logic
test: add integration tests for OAuth
```

**Commit types:**
- `feat` — New feature
- `fix` — Bug fix
- `docs` — Documentation only
- `refactor` — Code refactoring
- `test` — Test additions or updates
- `chore` — Build, CI, or tooling changes

### 5. Push and Create Pull Request

```bash
git push -u origin feature/48ID-E07-01-oauth-support
```

Then create a Pull Request on GitHub with:
- Clear title and description
- Reference to related issues/stories
- Screenshots/examples if applicable

## Code Standards

### Java Code Style

- **Formatting:** Follow Spring Boot conventions
- **Naming:**
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Spring Modulith Rules

48ID uses Spring Modulith to enforce module boundaries:

```
io.k48.fortyeightid/
├── auth/          → Authentication module
├── identity/      → Identity management module
├── admin/         → Admin operations module
├── provisioning/  → User provisioning module
├── audit/         → Audit logging module
└── shared/        → Shared infrastructure
```

**Rules:**
- ✅ Modules can expose public APIs through ports/interfaces
- ✅ Cross-module communication via public ports only
- ❌ Never access `internal/` packages of other modules
- ❌ No circular dependencies between modules

Verified by: `ApplicationModularityTests`

### Testing Standards

- **Unit tests:** Business logic with mocked dependencies
- **Integration tests:** Full flow with Testcontainers (use sparingly)
- **Coverage target:** 80%+ for new code

**Test naming:**
```java
void methodName_shouldExpectedBehavior_whenCondition() {
    // Given
    // When
    // Then
}
```

### Documentation Standards

When adding new features, update:

- ✅ API docs if endpoints change
- ✅ Guide docs if user-facing behavior changes
- ✅ Inline code comments for complex logic
- ✅ README if setup changes
- ✅ CHANGELOG if applicable

See the [PR checklist template](.github/pull_request_template.md).

## Project Structure

```
48id/
├── src/main/java/io/k48/fortyeightid/     # Application code
│   ├── auth/                              # Authentication module
│   ├── identity/                          # Identity module
│   ├── admin/                             # Admin module
│   ├── provisioning/                      # Provisioning module
│   ├── audit/                             # Audit module
│   └── shared/                            # Shared infrastructure
├── src/main/resources/                    # Configuration
│   ├── application.properties             # Main config
│   ├── db/migration/                      # Flyway migrations
│   └── templates/                         # Email templates
├── src/test/java/                         # Tests
├── docs/                                  # Documentation
│   ├── guide/                             # User guides
│   ├── api/                               # API reference
│   └── developers/                        # Developer docs
├── docker-compose.yml                     # Local infrastructure
└── build.gradle                           # Build configuration
```

## Detailed Developer Guide

For comprehensive development workflows, see:

→ **[Developer Guide](docs/developers/contributing.md)**

This includes:
- Module architecture details
- Testing strategies
- Security guidelines
- Performance considerations

For implementing user stories from the backlog:

→ **[Story Implementation Workflow](docs/developers/story-workflow.md)**

## Getting Help

- **Questions?** Open a GitHub Discussion
- **Bug?** Open a GitHub Issue
- **Feature idea?** Open a GitHub Issue with [Feature Request] tag
- **Security issue?** Email security@k48.io (do not open public issue)

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on the code, not the person
- Help others learn and grow

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).

---

Thank you for contributing to 48ID! 🚀
