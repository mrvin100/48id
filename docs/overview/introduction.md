# Introduction

## What is 48ID?

48ID is the identity and authentication service for the K48 ecosystem. It centralizes authentication, JWT token management, user provisioning, role-based authorization, and audit logging so that other K48 applications can rely on a single, consistent identity layer instead of implementing these concerns independently.

## MVP scope

The MVP implemented in this repository includes:

- account provisioning through CSV import
- activation of provisioned accounts by email token
- sign-in with JWT access tokens and refresh tokens
- password change and password reset flows
- self-service profile retrieval and update
- admin user lifecycle operations
- admin API key creation, listing, revocation, and rotation
- audit log retrieval and login history review
- trusted application endpoints for token verification and public identity lookup
- JWKS publication for JWT signature validation

The MVP does **not** include OAuth client registration, external identity providers, SSO federation, SCIM, or multi-tenant controls.

## Primary actors

- **Student**: authenticates, activates an account, changes password, updates profile, accesses K48 applications.
- **Administrator**: manages users, reviews audit trails, provisions accounts, and manages integration API keys.
- **Trusted application**: validates user tokens and queries limited identity data using `X-API-Key`.

## Product boundary

48ID is not a frontend identity portal. It is a backend platform that exposes HTTP APIs and sends transactional emails. User-facing applications such as 48Hub or LP48 integrate with it.

## Core design goals

- one identity source for the K48 ecosystem
- explicit module boundaries inside the backend
- secure default behavior for tokens, passwords, and administrative actions
- operational transparency through audit logs
- extensible documentation and architecture for future phases
