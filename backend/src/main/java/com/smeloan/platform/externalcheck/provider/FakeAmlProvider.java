package com.smeloan.platform.externalcheck.provider;

import com.smeloan.platform.externalcheck.entity.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fake AML/PEP screening provider for development and test environments.
 *
 * <p>Risk distribution:
 * <ul>
 *   <li>~80% LOW risk</li>
 *   <li>~15% MEDIUM risk</li>
 *   <li>~5% HIGH risk</li>
 * </ul>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class FakeAmlProvider implements AmlScreeningClient {

    @Override
    public AmlCheckResult screen(AmlScreenRequest request) {
        simulateLatency();

        int roll = ThreadLocalRandom.current().nextInt(100);
        RiskLevel riskLevel;
        boolean pepMatch;
        List<String> matchedLists;

        if (roll < 80) {
            riskLevel = RiskLevel.LOW;
            pepMatch = false;
            matchedLists = Collections.emptyList();
        } else if (roll < 95) {
            riskLevel = RiskLevel.MEDIUM;
            pepMatch = false;
            matchedLists = List.of("OFAC_SDN");
        } else {
            riskLevel = RiskLevel.HIGH;
            pepMatch = true;
            matchedLists = List.of("OFAC_SDN", "UN_CONSOLIDATED");
        }

        String rawResponse = """
            {
              "provider": "FakeAML",
              "name": "%s",
              "idNumber": "%s",
              "riskLevel": "%s",
              "pepMatch": %b,
              "matchedLists": %s
            }
            """.formatted(
                request.name(),
                request.idNumber() != null ? request.idNumber() : "",
                riskLevel.name(),
                pepMatch,
                matchedLists.isEmpty() ? "[]" : "[\"%s\"]".formatted(String.join("\",\"", matchedLists))
            );

        log.debug("[FakeAML] name={} riskLevel={} pepMatch={}", request.name(), riskLevel, pepMatch);

        return new AmlCheckResult(riskLevel, pepMatch, matchedLists, rawResponse);
    }

    @Override
    public String getProviderName() {
        return "FakeAML";
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
