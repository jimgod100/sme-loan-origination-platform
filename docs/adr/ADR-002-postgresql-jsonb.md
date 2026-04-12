# ADR-002: PostgreSQL JSONB for Audit Payloads

**Status:** Accepted  
**Date:** 2024-01-01  
**Author:** jimgod100

---

## Context

The SME loan origination workflow calls three external check providers:

1. **JCIC Credit Bureau** — returns credit score, delinquency history, outstanding obligations
2. **AML Screening** — returns risk level, watchlist matches, PEP flags
3. **Business Registry** — returns company status, registered capital, directors

Each provider returns a different response format. In a real-world integration, these response schemas may also change over time as providers update their APIs. The platform needs to:

1. Store the **complete, verbatim request and response** for each check as a regulatory audit trail.
2. Allow **querying** into the stored data (e.g., "find all applications with AML risk level HIGH").
3. Avoid creating a rigid table schema that breaks whenever a provider changes their response format.

---

## Decision

**Store external check request and response payloads as PostgreSQL `JSONB` columns in `external_check_results`.**

```sql
CREATE TABLE external_check_results (
    ...
    request_payload   JSONB,
    response_payload  JSONB,
    ...
);

-- GIN index for efficient JSONB queries
CREATE INDEX idx_ext_check_response_gin
    ON external_check_results USING GIN (response_payload);
```

Separately, **extracted key fields** (e.g., `result_code`, `risk_level`, `summary`) are also stored as typed columns for common query paths.

---

## Alternatives Considered

### Option A: Normalized Tables per Provider

Create separate tables for each provider's response:
- `jcic_check_results (credit_score, delinquency_count, ...)`
- `aml_check_results (risk_level, watchlist_flag, ...)`
- `registry_check_results (company_status, registered_capital, ...)`

**Rejected because:**
- Adding a new provider requires a schema migration.
- Provider field additions/changes require migrations even for fields the application doesn't use.
- The audit trail becomes fragmented across tables.
- This is exactly the kind of schema that frequently becomes a maintenance burden in real lending systems.

### Option B: Separate `VARCHAR(MAX)` / `TEXT` column for raw JSON

Store the JSON string as plain text.

**Rejected because:**
- No query capability — the database cannot index or filter on values inside the payload.
- No validation — any string can be stored, not just valid JSON.
- PostgreSQL `JSONB` is purpose-built for this use case and has minimal overhead vs. `TEXT`.

### Option C: Store in a separate document store (MongoDB, Elasticsearch)

Use a dedicated document database for check results.

**Rejected because:**
- Adds a third data store to the infrastructure (PostgreSQL + MinIO + document DB).
- Transactional consistency with the main application data becomes complex.
- For this scale and use case, JSONB in PostgreSQL provides sufficient document storage capabilities.

---

## Rationale

**Why JSONB specifically (vs. JSON):**
- `JSONB` stores a decomposed binary representation — it is **indexable** and **faster to query** than the plain `JSON` type.
- GIN indexes on JSONB support efficient containment queries: `response_payload @> '{"risk_level": "HIGH"}'`.
- `JSON` preserves insertion order and duplicates but cannot be indexed — it is only appropriate for pure pass-through storage.

**Dual-storage pattern:** The combination of full JSONB payload (for auditability and flexibility) plus extracted typed columns (for performance-critical queries) provides the best of both approaches:
- Compliance: full payload is preserved verbatim, signed by `requested_at` / `completed_at` timestamps.
- Performance: common filter paths (`result_code`, `risk_level`) use regular B-tree indexes.

---

## Consequences

**Positive:**
- New external check providers can be added without schema migrations.
- The complete audit trail is always available for compliance review.
- JSONB GIN index enables efficient searches across response data.
- Schema changes by providers are handled gracefully — unmapped fields are preserved in the JSONB payload.

**Negative / Trade-offs:**
- JSONB columns do not enforce a schema — application code is responsible for validating the structure before storage.
- Reporting queries that join deeply into JSONB fields can be verbose and slower than equivalent relational queries.
- Developers unfamiliar with JSONB operators (`->`, `->>`, `@>`, `#>>`) face a learning curve.

---

## References

- [PostgreSQL JSONB documentation](https://www.postgresql.org/docs/current/datatype-json.html)
- [PostgreSQL GIN indexes](https://www.postgresql.org/docs/current/gin.html)
