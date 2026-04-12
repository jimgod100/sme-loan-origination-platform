package com.smeloan.platform.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of {@link NotificationService} that simply logs
 * the notification details.
 *
 * <p>Replace with a real implementation (e.g. SendGrid for email,
 * Twilio for SMS) before deploying to production.</p>
 */
@Slf4j
@Service
public class MockNotificationService implements NotificationService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[MockEmail] To: {} | Subject: {} | Body length: {} chars",
            to, subject, body != null ? body.length() : 0);
        log.debug("[MockEmail] Body: {}", body);
    }

    @Override
    public void sendSms(String phone, String message) {
        log.info("[MockSMS] To: {} | Message: {}", phone, message);
    }
}
