# Architecture Overview

## SME Loan Origination Platform

---

## High-Level Diagram

```
┌──────────────────┐     ┌──────────────────┐
│  Internal Portal │     │ Customer Portal  │
│  React+TS+AntD   │     │ React+TS+Tailwind│
│     :3000        │     │     :3001        │
└────────┬─────────┘     └────────┬─────────┘
         │ REST + JWT             │ REST + Token
         └──────────┬─────────────┘
                    ▼
         ┌──────────────────────┐
         │   Spring Boot 3.x    │
         │   Java 17 · :8080    │
         │                      │
         │  auth | application  │
         │  onboarding | checks │
         │  scoring | disburse  │
         └──────┬───────┬───────┘
                │       │
       ┌────────┘       └───────────┐
       ▼                            ▼
┌─────────────┐           ┌─────────────────┐
│ PostgreSQL  │           │   MinIO (S3)    │
│     :5432   │           │  File Storage   │
└─────────────┘           └─────────────────┘
```

---

## Architectural Style: Modular Monolith

The backend is structured as a **Modular Monolith** — a single deployable Spring Boot application composed of distinct, bounded domain modules that enforce internal API contracts.

This approach was chosen over microservices for this portfolio project because:
- It keeps the deployment simple (one `docker-compose up`) while demonstrating clean architecture principles.
- Module boundaries are enforced by package structure and dependency rules, not network calls.
- Modules can be extracted into independent services later if required (see ADR-001).

---

## Backend Module Boundaries

```
com.jimgod100.smeloan/
├── auth/              # Authentication & authorization
├── application/       # Loan application lifecycle & state machine
├── onboarding/        # Customer token-based onboarding wizard
├── checks/            # External check orchestration (JCIC, AML, registry)
├── scoring/           # Rule-based credit scoring engine
├── disbursement/      # Loan offer, amortization, PDF generation
└── common/            # Shared utilities, exceptions, base entities
```

### Module Responsibilities

| Module | Responsibility | Key Entities |
|--------|---------------|--------------|
| `auth` | JWT issuance, validation, RBAC enforcement | `User`, `JwtTokenProvider` |
| `application` | Case creation, status transitions, RM console API | `LoanApplication`, `Customer`, `Document` |
| `onboarding` | Customer wizard steps, token validation, file upload | `OnboardingToken`, `ApplicationParty` |
| `checks` | External check orchestration via Strategy Pattern | `ExternalCheckResult`, `CheckProvider` |
| `scoring` | Rule evaluation, score calculation, auto-decision | `ScoreResult`, `ScoringEngine` |
| `disbursement` | Offer generation, amortization schedule, PDF output | `LoanOffer`, `RepaymentSchedule` |
| `common` | DTOs, exceptions, pagination, audit utilities | — |

---

## Cross-Cutting Concerns

### Authentication & Authorization

- **Internal users** (RM, MANAGER, ADMIN) authenticate via `POST /api/auth/login` and receive a short-lived JWT.
- **Customers** access the onboarding portal via a one-time URL containing a `token` path variable. No password required.
- Spring Security filters validate both JWT and customer tokens on each request.
- Role-based access control (RBAC) is enforced at the controller level using `@PreAuthorize`.

### Audit Trail

Every external check request and response is stored verbatim as a JSONB snapshot in `external_check_results`. This provides a complete, immutable audit trail without coupling the schema to any specific provider's response format (see ADR-002).

### Asynchronous Processing

Module decoupling uses **Spring Application Events**. For example:
- When onboarding completes, `OnboardingCompletedEvent` is published.
- The `checks` module listens and triggers all three external checks in parallel.
- When checks finish, `ChecksCompletedEvent` triggers the scoring module.

This avoids direct module-to-module method calls and keeps the event flow easy to trace.

### File Storage

Documents uploaded during onboarding are stored in **MinIO** (S3-compatible). The database stores only the object key and metadata. MinIO is accessible at port 9000 (API) and 9001 (web console).

---

## Database Design

The schema (see `V1__init_schema.sql`) consists of 9 tables:

| Table | Purpose |
|-------|---------|
| `users` | Internal staff accounts |
| `customers` | SME borrower profiles |
| `loan_applications` | Core loan case entity with status machine |
| `application_parties` | Directors, guarantors, shareholders |
| `documents` | File metadata (object stored in MinIO) |
| `external_check_results` | JCIC, AML, registry check results with JSONB payloads |
| `score_results` | Scoring engine output with dimension breakdown |
| `loan_offers` | Approved loan terms |
| `repayment_schedules` | Amortization schedule rows |

All tables use UUID primary keys, TIMESTAMPTZ for timestamps, and follow a `created_at / updated_at` audit pattern.

---

## Frontend Architecture

### Internal Portal (`frontend-internal`)
- **Framework:** React 18 + TypeScript
- **UI Library:** Ant Design
- **Port:** 3000
- **Key Views:** Application list, case detail, scoring result, manual review, offer generation

### Customer Portal (`frontend-customer`)
- **Framework:** React 18 + TypeScript
- **UI Library:** Tailwind CSS
- **Port:** 3001
- **Key Views:** 4-step onboarding wizard (Identity → Financials → Documents → Consent)

Both frontends communicate exclusively through the backend REST API. There is no direct frontend-to-database communication.

---

## Related Documents

- [ADR-001: Modular Monolith](adr/ADR-001-modular-monolith.md)
- [ADR-002: PostgreSQL JSONB](adr/ADR-002-postgresql-jsonb.md)
- [ADR-003: JWT + Customer Token](adr/ADR-003-jwt-stateless.md)
- [ADR-004: Strategy Pattern](adr/ADR-004-strategy-pattern.md)
- [Entity Relationship Diagram](erd.mmd)
