# SME Loan Origination Platform

> A full-stack digital lending workflow demo — from case creation and KYC/AML checks to credit scoring and disbursement.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue?logo=typescript)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Overview

This project demonstrates an end-to-end **SME (Small & Medium Enterprise) loan origination system**, designed as a portfolio showcase of system design, clean architecture, and full-stack development skills.

The platform covers the complete lending lifecycle:

1. **Case Creation** — Relationship Managers create loan applications with company profiles
2. **Customer Onboarding** — Token-based verification wizard for SME customers (identity, documents, consent)
3. **External Checks** — Mocked JCIC credit bureau, AML screening, and business registry queries
4. **Credit Scoring** — Rule-based scoring engine with configurable weights and automated decisioning
5. **Disbursement** — Loan offer generation, amortization schedule, and PDF contract output

## Architecture

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

## Key Features

### Loan Lifecycle (14+ states)
- Case creation by RM with company profile and credit requirements
- Token-based customer onboarding wizard (identity → financials → documents → consent)
- Asynchronous external checks: JCIC credit bureau, AML screening, business registry (all mocked with Strategy Pattern)
- Rule-based credit scoring engine with configurable weights and score breakdown
- Manual review workflow for borderline cases (MANAGER role)
- Loan offer generation with amortization schedule calculation
- PDF contract generation

### Technical Highlights
- **Modular Monolith** with Clean Architecture — 7 domain modules with clear boundaries
- **Strategy Pattern** for pluggable external check providers (easily swap mock ↔ real)
- **Complete audit trail** with request/response JSONB snapshots
- **JWT + RBAC** (RM, MANAGER, ADMIN) + token-based customer access
- **Spring Events** for async module decoupling
- **Flyway** managed database migrations
- **Testcontainers** integration tests
- **Docker Compose** one-command local setup

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17 + Spring Boot 3.2 |
| Frontend (Internal) | React 18 + TypeScript + Ant Design |
| Frontend (Customer) | React 18 + TypeScript + Tailwind CSS |
| Database | PostgreSQL 15 + Flyway |
| ORM | Spring Data JPA + Hibernate |
| Auth | Spring Security + JWT |
| File Storage | MinIO (S3-compatible) |
| PDF Generation | Apache PDFBox |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Testing | JUnit 5 + Mockito + Testcontainers |
| CI/CD | GitHub Actions |
| Container | Docker Compose |

## Quick Start

**Prerequisites:** Docker 24+, Docker Compose v2+

```bash
# Clone
git clone https://github.com/jimgod100/sme-loan-origination-platform.git
cd sme-loan-origination-platform

# Start all services
docker-compose up -d

# Verify services
docker-compose ps
```

**Access Points:**

| Service | URL | Credentials |
|---------|-----|-------------|
| Internal Portal (RM) | http://localhost:3000 | rm_demo / demo1234 |
| Customer Portal | http://localhost:3001/{token} | (via RM-generated URL) |
| Backend API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |

## Demo Walkthrough

1. Login as `rm_demo` at http://localhost:3000
2. Create a new application with sample company UBN `12345678`
3. Generate customer URL — copy the token link
4. Open the customer URL in a private window, complete the 4-step wizard
5. Watch the status update in the RM Console as checks run asynchronously
6. View the score breakdown after scoring completes
7. As `manager_demo`, approve the application (if manual review required)
8. Generate loan offer and download the PDF contract

## Project Structure

```
sme-loan-origination-platform/
├── backend/                  # Spring Boot application
│   ├── src/main/java/        # Java source (7 modules)
│   ├── src/main/resources/   # Config + Flyway migrations
│   └── src/test/             # Unit + integration tests
├── frontend-internal/        # RM Console (React + Ant Design)
├── frontend-customer/        # Customer Portal (React + Tailwind)
├── docs/                     # Architecture docs, ERD, ADRs
├── docker-compose.yml        # One-command local setup
└── .github/workflows/        # CI/CD pipeline
```

## API Documentation

Full API documentation available at **http://localhost:8080/swagger-ui.html** after starting the application.

Key endpoint groups:
- `POST /api/auth/login` — JWT authentication
- `GET/POST /api/applications` — Loan application CRUD
- `GET/PUT/POST /api/onboarding/{token}/*` — Customer verification flow
- `GET /api/applications/{id}/checks` — External check results
- `GET /api/applications/{id}/score-result` — Credit scoring
- `POST /api/applications/{id}/offer` — Loan offer & disbursement

## Architecture Decision Records

| ADR | Decision |
|-----|----------|
| [ADR-001](docs/adr/ADR-001-modular-monolith.md) | Modular Monolith over Microservices |
| [ADR-002](docs/adr/ADR-002-postgresql-jsonb.md) | PostgreSQL JSONB for audit payloads |
| [ADR-003](docs/adr/ADR-003-jwt-stateless.md) | Stateless JWT + customer token design |
| [ADR-004](docs/adr/ADR-004-strategy-pattern.md) | Strategy Pattern for external providers |

## Development

```bash
# Backend (requires Java 17+)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend Internal
cd frontend-internal && npm install && npm run dev

# Frontend Customer  
cd frontend-customer && npm install && npm run dev

# Run tests
cd backend && mvn verify
```

## License

MIT License — see [LICENSE](LICENSE) for details.

---

> **Disclaimer:** This is a technical demo project for portfolio purposes. All company data, financial figures, and credit check results are entirely fictional. This project is not affiliated with, endorsed by, or connected to any real financial institution, credit bureau, or AML screening service.
