# ADR-003: Stateless JWT + Customer Token Design

**Status:** Accepted  
**Date:** 2024-01-01  
**Author:** jimgod100

---

## Context

The platform has **two distinct user populations** with different authentication needs:

1. **Internal staff** (Relationship Managers, Credit Managers, Admins) — authenticated employees who log in with username and password via the Internal Portal.

2. **SME Customers** — business owners who receive an invitation link from their RM and complete a multi-step onboarding wizard. They do not have a pre-existing account, a username, or a password in the system.

These two populations require different authentication mechanisms, and the design must:
- Be stateless (no server-side session storage) to support horizontal scaling.
- Not require customers to register an account or remember a password.
- Allow RMs to control customer access (generate tokens, revoke access if needed).
- Be simple enough to implement securely within a portfolio project.

---

## Decision

**Use two complementary authentication mechanisms:**

### 1. JWT for Internal Staff

Internal users authenticate with `POST /api/auth/login` (username + password). On success, the server issues a signed **JWT** (JSON Web Token) with:
- `sub`: username
- `roles`: list of assigned roles (`RM`, `MANAGER`, `ADMIN`)
- `iat`: issued at
- `exp`: expiry (24 hours by default)

The JWT is signed with an HMAC-SHA256 key. All subsequent API requests include the token in the `Authorization: Bearer <token>` header. The server validates the signature and extracts claims from the token without any database lookup, making it fully stateless.

### 2. Opaque Token for Customers

When a Relationship Manager creates a loan application and initiates onboarding, the system:
1. Generates a cryptographically random, URL-safe token (UUID-based, 128 characters).
2. Stores the token and its expiry timestamp in `loan_applications.onboarding_token`.
3. Returns a customer-facing URL: `https://customer.smeloan.local/{token}`.

The customer portal reads the token from the URL path. Every API call to `/api/onboarding/{token}/*` passes the token as a path variable. The backend validates the token by querying the database:
- Token must exist and match an application.
- Token must not be expired.
- The application must be in a state that accepts onboarding steps.

---

## Alternatives Considered

### Option A: Server-side Sessions (HttpSession)

Store authenticated state in server memory or a session store (Redis).

**Rejected because:**
- Requires sticky sessions or a shared session store for horizontal scaling.
- Adds Redis as a required infrastructure dependency.
- JWT is the de facto standard for REST APIs and is more appropriate for a modern full-stack demo.

### Option B: OAuth 2.0 / OpenID Connect for Customers

Issue a short-lived OAuth access token to customers after they "claim" their token via a one-click flow.

**Rejected because:**
- Substantial added complexity (authorization server, PKCE flow, token refresh).
- Customers in this context do not have an existing identity provider account.
- The customer onboarding session is inherently short-lived and single-use — a simple database-validated opaque token is sufficient and easier to revoke.

### Option C: Magic Link via Email

Send a one-time email link to the customer's business email.

**Rejected because:**
- Requires an email delivery integration (SMTP/SES) which adds infrastructure.
- Customers may not have their business email readily accessible during an RM-led onboarding call.
- The RM-to-customer handoff model (verbal communication, QR code, or copy-paste) is more realistic for SME lending workflows.

### Option D: JWT for Customers too

Issue a JWT to customers after they present their onboarding token.

**Considered but not implemented:** The token-in-path design is simpler for the customer (no token management required in the SPA) and the token's validity is controlled by the server (stored in DB, can be invalidated instantly). For a longer-lived customer session, a JWT handoff would be appropriate.

---

## Security Considerations

| Concern | Mitigation |
|---------|-----------|
| JWT secret exposure | Secret is injected via environment variable, not committed to source |
| JWT theft | Short expiry (24h) + HTTPS in production |
| Customer token brute force | 128-character random token with UUID entropy; rate limiting recommended for production |
| Customer token reuse | Token is marked `onboarding_completed_at` and further steps are rejected after completion |
| Customer token expiry | Default 7 days; configurable via `app.jwt.customer-token-expiration-ms` |

---

## Consequences

**Positive:**
- No server-side session storage — stateless and horizontally scalable.
- Customer UX is frictionless: one URL, no registration.
- RM controls the onboarding lifecycle (generate, share, track completion).
- Token invalidation is immediate (delete from DB).

**Negative / Trade-offs:**
- JWT cannot be revoked before expiry without adding a denylist (e.g., Redis). For this demo, 24-hour expiry is an accepted trade-off.
- Customer token is visible in the URL — this is intentional (shareable link) but means the link should be treated as sensitive and shared over a secure channel.
