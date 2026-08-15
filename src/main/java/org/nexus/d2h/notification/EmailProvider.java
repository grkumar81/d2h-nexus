package org.nexus.d2h.notification;

public interface EmailProvider {
    /**
     * Send an email. Throws RuntimeException on failure.
     *
     * @param to      recipient email address
     * @param subject email subject
     * @param body    plain-text body
     */
    void send(String to, String subject, String body);
}
