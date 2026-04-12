package com.smeloan.platform.notification.service;

/**
 * Service interface for sending notifications to customers and internal staff.
 */
public interface NotificationService {

    /**
     * Sends an email notification.
     *
     * @param to      the recipient's email address
     * @param subject the email subject line
     * @param body    the email body (plain text or HTML)
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Sends an SMS notification.
     *
     * @param phone   the recipient's phone number (in E.164 format preferred)
     * @param message the SMS message text
     */
    void sendSms(String phone, String message);
}
