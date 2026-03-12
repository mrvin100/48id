# Security guide

## Security model

48ID combines several controls in the MVP:

- JWT bearer authentication for users
- refresh token rotation for session continuity
- API key authentication for trusted backend endpoints
- role-based authorization with method security
- password policy enforcement
- rate limiting
- audit logging
- Problem Details for consistent security error handling

## Token model

### Access token

- signed JWT
- short-lived
- used in `Authorization: Bearer` header

### Refresh token

- opaque token lifecycle managed server-side
- used only on refresh and logout operations
- rotated on refresh
- revoked on password reset

### API key

- managed by administrators
- used only by trusted applications
- sent in `X-API-Key`
- supports creation, rotation, listing, and revocation

## Rate limiting

The MVP defines these protections:

- 5 login attempts per 15 minutes per matricule
- 3 forgot-password requests per hour per email
- 100 requests per minute per IP globally

## Audit logging

48ID records audit-relevant events such as:

- login activity
- password reset activity
- account activation
- CSV import activity
- admin lifecycle changes

## Password handling

- passwords are hashed server-side
- password policy is enforced for change and reset flows
- initial temporary passwords must be replaced after first login

## Activation and reset token handling

- activation and reset tokens are time-bound
- tokens are single-use
- different token purposes are tracked separately in persistence

## Headers and transport

For non-local deployments:

- require HTTPS end to end or at ingress
- protect JWTs and API keys from client-side exposure
- configure CORS carefully for allowed origins only
