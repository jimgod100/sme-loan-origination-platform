package com.smeloan.platform.externalcheck.provider;

import com.smeloan.platform.externalcheck.entity.CheckType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fake JCIC credit bureau provider for development and test environments.
 * Generates random but realistic-looking credit data and simulates network
 * latency of 1–3 seconds.
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class FakeJcicProvider implements CreditBureauClient {

    private static final int SCORE_MIN = 400;
    private static final int SCORE_MAX = 900;

    @Override
    public CreditCheckResult query(CreditCheckRequest request) {
        simulateLatency();

        Random rnd = ThreadLocalRandom.current();
        int creditScore = rnd.nextInt(SCORE_MIN, SCORE_MAX + 1);
        int delinquencyCount = weightedDelinquency(rnd, creditScore);
        double debtRatio = Math.round(rnd.nextDouble(0.1, 0.9) * 100.0) / 100.0;

        String rawResponse = """
            {
              "provider": "FakeJCIC",
              "ubn": "%s",
              "creditScore": %d,
              "delinquencyCount": %d,
              "debtRatio": %.2f
            }
            """.formatted(request.ubn(), creditScore, delinquencyCount, debtRatio);

        log.debug("[FakeJCIC] UBN={} score={} delinquency={} debtRatio={}",
            request.ubn(), creditScore, delinquencyCount, debtRatio);

        return new CreditCheckResult(creditScore, delinquencyCount, debtRatio, rawResponse);
    }

    @Override
    public String getProviderName() {
        return "FakeJCIC";
    }

    @Override
    public CheckType getCheckType() {
        return CheckType.JCIC;
    }

    // ------------------------------------------------------------------

    private void simulateLatency() {
        long millis = ThreadLocalRandom.current().nextLong(1000, 3001);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Higher credit scores correlate with fewer delinquencies. */
    private int weightedDelinquency(Random rnd, int score) {
        if (score >= 750) return rnd.nextInt(0, 2);
        if (score >= 600) return rnd.nextInt(0, 4);
        return rnd.nextInt(0, 8);
    }
}
