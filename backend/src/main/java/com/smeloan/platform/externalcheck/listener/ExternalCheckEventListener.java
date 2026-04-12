package com.smeloan.platform.externalcheck.listener;

import com.smeloan.platform.application.event.ReadyForChecksEvent;
import com.smeloan.platform.externalcheck.service.ExternalCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Spring application event listener that triggers external checks when an
 * application transitions to the {@code READY_FOR_CHECKS} state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalCheckEventListener {

    private final ExternalCheckService externalCheckService;

    /**
     * Handles the {@link ReadyForChecksEvent} by asynchronously kicking off all
     * external checks for the application.
     *
     * @param event the event containing the application ID
     */
    @Async("externalCheckExecutor")
    @EventListener
    public void onReadyForChecks(ReadyForChecksEvent event) {
        log.info("Received ReadyForChecksEvent for applicationId={}", event.applicationId());
        externalCheckService.executeAllChecks(event.applicationId());
    }
}
