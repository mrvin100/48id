# Developer guide

## Contribution priorities for documentation

When changing functionality, update documentation in the same branch for:

- affected endpoints
- changed request or response schemas
- security behavior changes
- new environment variables
- new operational workflows

## Documentation maintenance rules

- keep docs aligned with implemented behavior, not planned behavior
- document new endpoints in the appropriate API page
- update diagrams when flows or module boundaries change
- avoid duplicating the same normative statement in many files
- prefer linking from overview pages to detailed reference pages

## Recommended update workflow

1. update code
2. update tests
3. update docs in `docs/`
4. verify Swagger/OpenAPI output still matches the written reference
5. include documentation changes in code review

## Coding guidance for future maintainers

- preserve module boundaries between `auth`, `identity`, `admin`, `provisioning`, and `audit`
- prefer ports/services over cross-module repository access
- keep API key–protected endpoints distinct from bearer-token endpoints
- extend Problem Details consistently for new error cases

## See also

- root [`CONTRIBUTING.md`](../../CONTRIBUTING.md)
- [Architecture](../overview/architecture.md)
- [Error model](../api/errors.md)
