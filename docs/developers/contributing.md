# Developer guide

This guide covers contribution workflows and coding standards for 48ID.

## Story implementation workflow

For a complete step-by-step guide to implementing user stories from the MVP backlog, see:

→ **[Story implementation workflow](story-workflow.md)**

This includes:
- Git branching strategy
- Implementation planning
- Testing requirements
- Commit and PR conventions
- Troubleshooting common issues

## Documentation maintenance

When changing functionality, update documentation in the same branch for:

- affected endpoints
- changed request or response schemas
- security behavior changes
- new environment variables
- new operational workflows

### Documentation rules

- keep docs aligned with implemented behavior, not planned behavior
- document new endpoints in the appropriate API page
- update diagrams when flows or module boundaries change
- avoid duplicating the same normative statement in many files
- prefer linking from overview pages to detailed reference pages

### Recommended update workflow

1. update code
2. update tests
3. update docs in `docs/`
4. verify Swagger/OpenAPI output still matches the written reference
5. include documentation changes in code review

See the [PR checklist template](../../.github/pull_request_template.md) for full guidance.

## Coding standards

### Module boundaries

- preserve module boundaries between `auth`, `identity`, `admin`, `provisioning`, and `audit`
- prefer ports/services over cross-module repository access
- keep API key–protected endpoints distinct from bearer-token endpoints
- extend Problem Details consistently for new error cases

### Naming conventions

- **Classes:** `PascalCase` (e.g., `CsvImportService`)
- **Methods:** `camelCase` (e.g., `importUsers`)
- **Tests:** `method_shouldDoSomething`
- **Exceptions:** `SpecificException`

### Testing standards

- write unit tests for all business logic
- use Mockito for mocking dependencies
- aim for 80%+ code coverage on new code
- test all acceptance criteria
- test error cases and edge cases

## See also

- [Story implementation workflow](story-workflow.md)
- root [`CONTRIBUTING.md`](../../CONTRIBUTING.md)
- [Architecture](../guide/architecture.md)
- [Error model](../api/errors.md)
