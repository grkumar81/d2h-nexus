package org.nexus.d2h.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StubWhatsAppProvider implements WhatsAppProvider {

    @Override
    public void send(String to, String message) {
        log.info("[STUB WHATSAPP] to={} message={}", to, message);
    }
}
