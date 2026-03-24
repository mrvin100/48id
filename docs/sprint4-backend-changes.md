# Sprint 4 — Backend Changes Documentation

## Overview

This document covers all backend changes made during Sprint 4, across two feature branches:

- `feature/48ID-WEB-S4-BE-05-operator-role-seed` — merged to `main` via PR #65
- `feature/48ID-WEB-S4-BE-01-matricule-validation-batch-filter` — current branch, ready to merge

---

## WEB-S4-BE-05 — OPERATOR Role & Module Foundation

### What was the problem?

The system had two roles: `ADMIN` and `STUDENT`. There was no role for operators (external partners who consume the API). Controllers referenced `SYSTEM_OPERATOR` which didn't exist in the database, causing authorization failures.

### What was done?

#### 1. Flyway migration — `V8__add_operator_role.sql`
Seeds the `OPERATOR` role into the `roles` table at startup.

```sql
INSERT INTO roles (name) VALUES ('OPERATOR') ON CONFLICT DO NOTHING;
```

Every time the application starts on a fresh or existing database, this migration ensures the `OPERATOR` role exists.

#### 2. New Spring Modulith module — `operator/`

A new first-class module was created at `io.k48.fortyeightid.operator`. Spring Modulith enforces that modules only talk to each other through declared public ports — this keeps the codebase from becoming a tangle of cross-module dependencies.

The module is allowed to depend on: `shared`, `identity`, `audit`, `admin`.

```
operator/
├── package-info.java                        ← module boundary declaration
└── internal/
    ├── OperatorDashboardController.java     ← GET /operator/dashboard/metrics
    ├── OperatorAuditController.java         ← GET /operator/audit-log
    ├── OperatorUserController.java          ← GET /operator/users, GET /operator/users/{id}
    ├── OperatorDashboardResponse.java       ← response record (package-private)
    ├── OperatorAuditLogResponse.java        ← response record (package-private)
    └── OperatorUserResponse.java            ← response record with from(User) factory
```

All three controllers are secured with `@PreAuthorize("hasRole('OPERATOR')")` — only users with the `OPERATOR` role can call these endpoints.

#### 3. Public port on `admin` — `DashboardQueryPort.java`

The `operator` module needs to read dashboard data and user data that lives in the `admin` module. In Spring Modulith, a module cannot directly call another module's internal classes — it must go through a public port (an interface in the module's root package).

`DashboardQueryPort` is that interface. It exposes:
- `getDashboardSnapshot()` — returns aggregated metrics
- `listUsers(status, batch, role, pageable)` — paginated user list
- `getUser(id)` — single user lookup

`AdminDashboardService` implements this port. The `operator` module calls the port, never the service directly.

#### 4. Bug fix — `AdminAuditController.java`

The existing controller referenced `SYSTEM_OPERATOR` in a `@PreAuthorize` annotation. This was corrected to `OPERATOR` to match the actual role name in the database.

---

## WEB-S4-BE-01 — Matricule Format Validation

### What was the problem?

Users could be created with any string as their matricule (e.g. `"hello"`, `"K48-2024-001"`). The system had no enforcement of the required format `K48-B{n}-{seq}` (example: `K48-B1-12`). It also didn't verify that the matricule's batch prefix matched the user's declared batch — so a user in batch `B1` could have a matricule starting with `K48-B2-`.

### What was done?

#### 1. `MatriculeValidator.java` — `shared/` module

A pure utility class (no Spring bean, no injection needed) with a single static method:

```java
MatriculeValidator.validate("K48-B2-5", "B1")
// → Optional.of("Matricule prefix 'K48-B2' does not match batch 'B1'")

MatriculeValidator.validate("K48-B1-12", "B1")
// → Optional.empty()  ← valid, no error
```

Two rules are enforced in order:
1. **Format check** — must match regex `^K48-B[0-9]+-[0-9]+$`
2. **Prefix check** — the batch embedded in the matricule (e.g. `B2` in `K48-B2-5`) must match the `batch` field (e.g. `B1`)

It lives in `shared/` because it is an OPEN module — accessible to all other modules without any dependency declaration.

#### 2. `InvalidMatriculeFormatException.java` — `shared/exception/`

A plain `RuntimeException` subclass, following the same pattern as `DuplicateMatriculeException` and all other domain exceptions in the project.

```java
throw new InvalidMatriculeFormatException("Matricule prefix 'K48-B2' does not match batch 'B1'");
```

#### 3. `GlobalExceptionHandler.java` — new handler entry

When `InvalidMatriculeFormatException` is thrown anywhere in the application, Spring catches it here and returns a structured RFC 9457 `ProblemDetail` response:

```json
{
  "status": 400,
  "title": "Invalid Matricule Format",
  "detail": "Matricule prefix 'K48-B2' does not match batch 'B1'",
  "type": "https://48id.k48.io/errors/invalid-matricule-format",
  "code": "INVALID_MATRICULE_FORMAT",
  "timestamp": "2026-03-24T08:00:00Z"
}
```

The `code` field (`INVALID_MATRICULE_FORMAT`) is what the frontend uses to display the correct localized error message.

#### 4. `UserProvisioningService.java` — validation wired in

Validation is called as the very first thing in `createUser()`, before any database checks:

```
validate matricule format  ← NEW (throws if invalid)
check matricule not duplicate
check email not duplicate
build and save user
```

This order matters: we fail fast on format errors before hitting the database.

#### 5. `BootstrapService.java` — validation wired in

Same validation added to `createFirstAdmin()`, after the admin-count check but before uniqueness checks:

```
check no admin exists yet
validate matricule format  ← NEW (throws if invalid)
check matricule not duplicate
check email not duplicate
build and save admin user
```

---

## WEB-S4-BE-02 — Batch Filter Verification

### What was the situation?

The `GET /admin/users?batch=B1` endpoint already existed and was already wired end-to-end:
- `AdminUserController` accepts `?batch=` as a request param
- `AdminUserService` passes it to `UserQueryService`
- `UserQueryService.findAll()` builds a JPA `Specification` that filters by batch when the param is present
- `UserRepository` has a `findByBatch(String batch, Pageable pageable)` method

There was no test proving this actually worked.

### What was done?

Added `UserRepositoryBatchFilterTest` — a `@DataJpaTest` test that spins up a real PostgreSQL container (via Testcontainers), inserts 3 users across 2 batches, and verifies:

1. `findByBatch("B1")` returns only the 2 B1 users
2. `findByBatch("B99")` returns an empty page
3. Pagination works correctly (page size 1 of 2 total B1 users)

---

## Test Coverage Added

| Test class | Type | Tests | What it covers |
|---|---|---|---|
| `MatriculeValidatorTest` | Unit | 5 | All validator branches: valid, null, bad format, prefix mismatch |
| `UserProvisioningServiceMatriculeTest` | Unit (Mockito) | 3 | Validation fires before DB calls in `createUser()` |
| `UserRepositoryBatchFilterTest` | `@DataJpaTest` (Testcontainers) | 3 | Batch filter + pagination on real DB |
| `BootstrapServiceTest` | Unit (Mockito) | — | Existing tests updated: stale `K48-2024-001` → `K48-B1-1` |

**Total: 143 tests, 0 failures**

---

## Error Message Contract (Frontend)

The exact error message format is fixed and must not change — the frontend depends on it:

```
"Matricule prefix '{actual_prefix}' does not match batch '{batch}'"
```

Example: `"Matricule prefix 'K48-B2' does not match batch 'B1'"`

---

## Files Changed — Full List

### WEB-S4-BE-05 (merged)
| File | Change |
|---|---|
| `src/main/resources/db/migration/V8__add_operator_role.sql` | Created |
| `src/main/java/.../admin/DashboardQueryPort.java` | Created |
| `src/main/java/.../admin/internal/AdminDashboardService.java` | Implements `DashboardQueryPort` |
| `src/main/java/.../admin/internal/AdminAuditController.java` | Fixed `SYSTEM_OPERATOR` → `OPERATOR` |
| `src/main/java/.../operator/package-info.java` | Created |
| `src/main/java/.../operator/internal/OperatorDashboardController.java` | Created |
| `src/main/java/.../operator/internal/OperatorAuditController.java` | Created |
| `src/main/java/.../operator/internal/OperatorUserController.java` | Created |
| `src/main/java/.../operator/internal/OperatorDashboardResponse.java` | Created |
| `src/main/java/.../operator/internal/OperatorAuditLogResponse.java` | Created |
| `src/main/java/.../operator/internal/OperatorUserResponse.java` | Created |
| `src/test/java/.../operator/internal/OperatorDashboardControllerTest.java` | Created |
| `src/test/java/.../operator/internal/OperatorAuditControllerTest.java` | Created |
| `src/test/java/.../operator/internal/OperatorUserControllerTest.java` | Created |

### WEB-S4-BE-01 + WEB-S4-BE-02 (current branch)
| File | Change |
|---|---|
| `src/main/java/.../shared/MatriculeValidator.java` | Created |
| `src/main/java/.../shared/exception/InvalidMatriculeFormatException.java` | Created |
| `src/main/java/.../shared/exception/GlobalExceptionHandler.java` | Added `INVALID_MATRICULE_FORMAT` handler |
| `src/main/java/.../identity/internal/UserProvisioningService.java` | Validation added before duplicate checks |
| `src/main/java/.../auth/internal/BootstrapService.java` | Validation added before uniqueness checks |
| `src/test/java/.../shared/MatriculeValidatorTest.java` | Created |
| `src/test/java/.../identity/internal/UserProvisioningServiceMatriculeTest.java` | Created |
| `src/test/java/.../identity/internal/UserRepositoryBatchFilterTest.java` | Created |
| `src/test/java/.../auth/internal/BootstrapServiceTest.java` | Updated stale test data |
