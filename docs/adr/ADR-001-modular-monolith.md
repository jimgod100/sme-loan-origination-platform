# ADR-001: Modular Monolith over Microservices

**Status:** Accepted  
**Date:** 2024-01-01  
**Author:** jimgod100

---

## Context

When beginning this portfolio project, the first architectural decision to make was whether to build the SME Loan Origination Platform as a **microservices** system or a **monolith**.

The loan origination domain naturally decomposes into distinct bounded contexts:
- Authentication
- Application management
- Customer onboarding
- External checks (JCIC, AML, business registry)
- Credit scoring
- Disbursement

These boundaries suggest microservices, which are often positioned as the "modern" default. However, microservices introduce significant operational complexity that needs to be justified.

---

## Decision

**Implement a Modular Monolith.**

The backend is a single Spring Boot application, structured into seven clearly bounded packages (`auth`, `application`, `onboarding`, `checks`, `scoring`, `disbursement`, `common`). Each module:
- Has its own service layer, repository layer, and API contracts.
- Does **not** directly call into another module's repository.
- Communicates with other modules via Spring Application Events or dedicated service interfaces.
- Could be extracted into a standalone service with bounded refactoring effort.

---

## Rationale

### Arguments for Modular Monolith

1. **Simplicity of deployment.** A single JAR, one `docker-compose up`, and the entire system is running. A recruiter or reviewer can evaluate the project in under five minutes.

2. **Portfolio readability.** The clean architecture and module separation are clearly visible in the repository structure. A microservices version would spread logic across multiple repositories, making it harder to evaluate in a single review session.

3. **No distributed systems overhead.** Microservices require service discovery (Consul/Eureka), API gateways, distributed tracing, inter-service authentication, circuit breakers, and retry logic. None of these add value for a demo project — they add noise.

4. **Data consistency is simpler.** The loan application state machine touches multiple tables in a single transaction. With microservices, maintaining consistency across service boundaries requires a Saga pattern or 2PC, both of which are significant complexity to implement correctly.

5. **Module boundaries are still enforced.** By convention and code review rules, modules do not directly import each other's internal packages. The boundaries are architectural, not just physical.

6. **Future extractability.** If this platform were to grow into a production system, the well-defined module boundaries make it straightforward to extract high-load services (e.g., the `checks` module that calls external APIs) into separate deployments.

### Arguments Considered for Microservices

| Argument | Counter-argument |
|----------|-----------------|
| Independent scaling per service | Loan origination is not high-throughput; scaling a monolith vertically is sufficient at this stage |
| Independent deployments per team | Single developer — no team coordination benefit |
| Technology heterogeneity | No need to mix languages/frameworks for a demo |
| Fault isolation | Risk of partial failure is a real-world concern but adds 3× more infrastructure code for a portfolio demo |

---

## Consequences

**Positive:**
- Fast local setup for any reviewer.
- Codebase is navigable in a single IDE window.
- No distributed transaction complexity.
- CI pipeline is a single `mvn verify`.

**Negative / Trade-offs:**
- A single large JAR is deployed — no per-module independent scaling.
- A bug in one module can crash the entire application (mitigated by thorough testing).
- Module boundary enforcement depends on developer discipline; there is no compile-time mechanism preventing cross-module package access.

---

## Notes

This decision does **not** mean microservices are wrong for loan origination systems. In a production environment with multiple teams and high throughput requirements, extracting the `checks` module (external API calls with variable latency) and the `scoring` module into separate services would be a natural evolution.
