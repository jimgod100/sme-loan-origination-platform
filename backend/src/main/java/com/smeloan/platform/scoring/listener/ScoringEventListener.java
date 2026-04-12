package com.smeloan.platform.scoring.listener;

import com.smeloan.platform.application.event.TriggerScoringEvent;
import com.smeloan.platform.scoring.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Spring application event listener that triggers credit scoring when all
 * external checks have completed and the application is ready to be scored.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringEventListener {

    private final ScoringService scoringService;

    /**
     * Handles the {@link TriggerScoringEvent} by running the scoring model
     * for the application.
     *
     * @param event the event containing the application ID
     */
    @Async("defaultTaskExecutor")
    @EventListener
    public void onTriggerScoring(TriggerScoringEvent event) {
        log.info("Received TriggerScoringEvent for applicationId={}", event.applicationId());
        try {
            scoringService.score(event.applicationId());
        } catch (Exception e) {
            log.error("Scoring failed for applicationId={}: {}", event.applicationId(), e.getMessage(), e);
        }
    }
}
