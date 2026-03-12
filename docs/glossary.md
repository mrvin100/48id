# Glossary

Key terms and concepts used in 48ID documentation.

## A

### Access Token
A short-lived JWT token (15 minutes) that grants access to protected resources. Signed with RS256 algorithm.

### API Key
A long-lived secret key used by external applications to authenticate with 48ID for server-to-server operations.

### Audit Log
Immutable record of significant events in the system, including authentication attempts, admin actions, and security events.

### Authentication
The process of verifying a user's identity, typically through credentials like matricule and password.

### Authorization
The process of determining what actions a user is allowed to perform based on their roles and permissions.

## B

### Bearer Token
An authentication token that grants access to whoever "bears" (possesses) it. JWT access tokens are bearer tokens.

### Bucket4j
A rate-limiting library used by 48ID to prevent abuse of authentication endpoints.

## C

### Client Application
An external application (like 48Hub or LP48) that integrates with 48ID for authentication and identity verification.

### CORS (Cross-Origin Resource Sharing)
A security mechanism that allows web applications from different origins to make requests to 48ID API.

## E

### Epic
A large body of work in the 48ID backlog, broken down into smaller stories. Example: "Authentication System" epic.

## I

### Identity Provider (IdP)
A system that creates, maintains, and manages identity information. 48ID is the IdP for the K48 ecosystem.

### IDaaS (Identity-as-a-Service)
Cloud-based identity and access management. 48ID functions as an internal IDaaS for K48.

## J

### JWT (JSON Web Token)
A compact, URL-safe token format used for securely transmitting information between parties. Used by 48ID for access tokens.

### JWKS (JSON Web Key Set)
A set of cryptographic keys used to verify JWT signatures. Available at `/.well-known/jwks.json`.

## K

### K48 Ecosystem
The collection of platforms and applications serving the KFOKAM48 community, including 48ID, 48Hub, LP48, and others.

## M

### Matricule
A unique identifier assigned to K48 students. Used as the primary username for authentication.

## O

### OpenAPI
A specification for machine-readable interface files for describing, producing, consuming, and visualizing web services. 48ID uses OpenAPI 3.0.

## P

### Provisioning
The process of creating and managing user accounts. 48ID supports CSV bulk provisioning.

## R

### Rate Limiting
A technique to control the number of requests a client can make within a time window. Protects against brute force attacks.

### Refresh Token
A long-lived token (7 days) used to obtain new access tokens without re-authentication. Stored in Redis.

### RBAC (Role-Based Access Control)
An access control mechanism where permissions are associated with roles. 48ID implements RBAC with ADMIN and STUDENT roles.

### RS256
An asymmetric signing algorithm (RSA with SHA-256) used for signing JWT access tokens in 48ID.

## S

### Spring Modulith
An architectural framework for building modular Spring Boot applications with enforced module boundaries.

### Story
A unit of work in Agile development. 48ID stories follow the format "As a [user], I want [feature], so that [benefit]".

## T

### Token Rotation
The process of issuing a new refresh token while invalidating the old one. Enhances security by limiting token reuse.

## U

### User Status
The current state of a user account. 48ID supports: ACTIVE, SUSPENDED, PENDING_ACTIVATION.

## X

### X-API-Key
HTTP header used to transmit API keys for authenticating external applications with 48ID.
