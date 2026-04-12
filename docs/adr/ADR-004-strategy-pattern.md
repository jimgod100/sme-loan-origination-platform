# ADR-004: Strategy Pattern for External Check Providers

**Status:** Accepted  
**Date:** 2024-01-01  
**Author:** jimgod100

---

## Context

The loan origination workflow requires three external data checks:

1. **JCIC Credit Bureau** — credit score and delinquency history
2. **AML Screening** — watchlist screening and PEP flagging
3. **Business Registry** — company status verification

In production, each of these would call a real third-party API (JCIC, AUSTRAC, local registry). However, for this portfolio project:
- Real API credentials are not available.
- Real calls would require paid subscriptions and complex legal agreements.
- The focus is on demonstrating system design, not the specifics of any single provider.

The code must be designed so that:
- The application works end-to-end using **mocked implementations**.
- Swapping a mock implementation for a real one requires **minimal code changes**.
- Adding a **new provider** (e.g., a different credit bureau) does not require modifying existing code.

---

## Decision

**Implement the external check providers using the Strategy Pattern, with Spring `@Profile` for environment-based provider selection.**

### Structure

```java
// Strategy interface (in checks module)
public interface CreditBureauProvider {
    CreditBureauResult check(CreditBureauRequest request);
    String getProviderName();
}

// Mock implementation (always available)
@Component
@Profile("!prod")
public class MockJcicProvider implements CreditBureauProvider {
    public CreditBureauResult check(CreditBureauRequest request) {
        // Simulate deterministic results based on UBN
        // Returns realistic-looking data without real API calls
    }
    public String getProviderName() { return "MOCK_JCIC_V1"; }
}

// Real implementation (activated only in production profile)
@Component
@Profile("prod")
public class RealJcicProvider implements CreditBureauProvider {
    // Calls actual JCIC API
    public String getProviderName() { return "JCIC_PROD_V1"; }
}
```

The same pattern applies to `AmlScreeningProvider` and `BusinessRegistryProvider`.

The `CheckOrchestrationService` depends only on the `CreditBureauProvider` interface — it never references a concrete implementation:

```java
@Service
public class CheckOrchestrationService {
    private final CreditBureauProvider creditBureauProvider;
    private final AmlScreeningProvider amlProvider;
    private final BusinessRegistryProvider registryProvider;

    // Spring injects the correct implementation based on active profile
    public CheckOrchestrationService(
            CreditBureauProvider creditBureauProvider,
            AmlScreeningProvider amlProvider,
            BusinessRegistryProvider registryProvider) {
        this.creditBureauProvider = creditBureauProvider;
        this.amlProvider = amlProvider;
        this.registryProvider = registryProvider;
    }
}
```

---

## Alternatives Considered

### Option A: `if/else` or `switch` on a configuration flag

```java
if (config.isUseMockProviders()) {
    result = mockJcicProvider.check(request);
} else {
    result = realJcicProvider.check(request);
}
```

**Rejected because:**
- Violates the Open-Closed Principle — adding a new provider requires modifying this conditional.
- Conditional logic is scattered across multiple services.
- No clean separation between the calling code and the provider implementation.

### Option B: Spring `@ConditionalOnProperty`

Activate beans based on a property key rather than a Spring `@Profile`.

**Considered:** This is a valid approach and arguably more flexible than `@Profile` for fine-grained configuration. However, `@Profile` is cleaner for environment-level switching (`dev`, `test`, `prod`) which aligns with the deployment model of this project.

### Option C: Factory Pattern

A `CheckProviderFactory.getProvider(CheckType)` method returns the appropriate implementation at runtime.

**Considered:** Useful when you need to switch providers dynamically at runtime (e.g., A/B testing different providers). For this project, provider selection is environment-level (deploy-time), making the Strategy + `@Profile` combination cleaner and sufficient.

---

## Mock Implementation Design

The mock providers generate **deterministic, realistic-looking results** based on the company's Unified Business Number (UBN):
- UBN ending in an even digit → `PASS` / low risk
- UBN ending in an odd digit → borderline or `REFER` result
- UBN `99999999` → always returns `FAIL` / high risk (useful for testing rejection flow)

This allows the demo walkthrough to be reproducible without an external dependency.

---

## Benefits of This Design

1. **Open-Closed Principle:** New providers are added by implementing the interface and annotating with `@Profile` — existing code is unchanged.

2. **Testability:** Unit tests for `CheckOrchestrationService` inject a mock (or stub) implementation. Integration tests use the `test` profile, which activates the mock providers automatically.

3. **Portfolio clarity:** A recruiter reviewing the codebase can see the interface contract and both implementations, demonstrating knowledge of design patterns, Spring DI, and the separation of concerns.

4. **Zero vendor lock-in:** The application is not wired to any specific provider's SDK. Switching from JCIC to an equivalent bureau is purely an implementation detail behind the interface.

---

## Consequences

**Positive:**
- Clean, swap-friendly provider architecture.
- Demo runs fully without real API credentials.
- Follows SOLID principles (OCP, DIP) demonstrably.
- Easy to test at all levels (unit, integration, end-to-end).

**Negative / Trade-offs:**
- Slight indirection — readers must follow the interface to understand the full behavior.
- `@Profile` Spring beans require care in testing to ensure the correct implementation is active.
