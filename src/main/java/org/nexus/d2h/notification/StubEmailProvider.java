package org.nexus.d2h.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(SmtpEmailProvider.class)
public class StubEmailProvider implements EmailProvider {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[STUB EMAIL] to={} subject={} body={}", to, subject, body);
    }
}
