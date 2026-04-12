package com.smeloan.platform.externalcheck.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fake enterprise tax compliance / business registry provider for development
 * and test environments.
 *
 * <p>Company status distribution:
 * <ul>
 *   <li>~90% ACTIVE</li>
 *   <li>~8% SUSPENDED</li>
 *   <li>~2% DISSOLVED</li>
 * </ul>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class FakeEtcProvider implements BusinessRegistryClient {

    private static final String[] INDUSTRIES = {
        "Manufacturing", "Retail Trade", "Information Technology",
        "Financial Services", "Construction", "Wholesale Trade",
        "Transportation", "Healthcare"
    };

    @Override
    public BusinessRegistryResult lookup(String ubn) {
        simulateLatency();

        int roll = ThreadLocalRandom.current().nextInt(100);
        String status;
        if (roll < 90) {
            status = "ACTIVE";
        } else if (roll < 98) {
            status = "SUSPENDED";
        } else {
            status = "DISSOLVED";
        }

        String industry = INDUSTRIES[ThreadLocalRandom.current().nextInt(INDUSTRIES.length)];
        String companyName = "Company-" + ubn;

        String rawResponse = """
            {
              "provider": "FakeETC",
              "ubn": "%s",
              "companyName": "%s",
              "status": "%s",
              "industry": "%s"
            }
            """.formatted(ubn, companyName, status, industry);

        log.debug("[FakeETC] UBN={} status={} industry={}", ubn, status, industry);

        return new BusinessRegistryResult(status, companyName, industry, rawResponse);
    }

    @Override
    public String getProviderName() {
        return "FakeETC";
    }

    private void simulateLatency() {
        long millis = ThreadLocalRandom.current().nextLong(500, 2001);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
