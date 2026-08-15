package org.nexus.d2h.notification;

public interface WhatsAppProvider {
    /**
     * Send a WhatsApp message. Throws RuntimeException on failure.
     *
     * @param to      recipient phone number (E.164 format, e.g. +919876543210)
     * @param message message text
     */
    void send(String to, String message);
}
