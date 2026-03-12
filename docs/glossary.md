# Glossary

## Identity

The representation of a person inside 48ID, including identifiers, profile data, lifecycle state, and assigned roles.

## Authentication

The process of verifying that a user or application is who it claims to be.

## Authorization

The process of determining which actions an authenticated user or application is allowed to perform.

## Access token

A short-lived signed JWT issued after login or refresh and used for bearer-authenticated API requests.

## Refresh token

A token used to obtain a new access token without asking the user to log in again.

## Client application

A user-facing or backend application that integrates with 48ID, such as 48Hub or LP48.

## API key

A raw secret created by an administrator for a trusted backend application. It is used in the `X-API-Key` header.

## Provisioning

The process of creating user accounts in 48ID, including the CSV import workflow in the MVP.

## Matricule

The institutional user identifier used as the primary login identifier in the MVP.

## JWKS

JSON Web Key Set. A standard document that publishes public keys used to validate JWT signatures.

## Account activation

The transition from `PENDING_ACTIVATION` to `ACTIVE` using an activation token sent by email.

## Profile completion

A flag indicating whether the user's essential profile information is considered complete for consuming applications.

## Audit log

A timestamped record of security-relevant or administrative actions performed in the system.
