package org.nexus.d2h.notification;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final NotificationConfigRepository configRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    public NotificationService(NotificationConfigRepository configRepository,
                                NotificationDeliveryRepository deliveryRepository) {
        this.configRepository = configRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationConfigDto> listConfigs() {
        return configRepository.findAll().stream().map(NotificationConfigDto::from).toList();
    }

    @Transactional
    public NotificationConfigDto saveConfig(SaveNotificationConfigRequest request) {
        NotificationConfig config = configRepository
                .findByEventTypeAndChannel(request.eventType(), request.channel())
                .orElseGet(() -> {
                    NotificationConfig c = new NotificationConfig();
                    c.setEventType(request.eventType());
                    c.setChannel(request.channel());
                    c.setCreatedBy(currentUsername());
                    return c;
                });

        config.setEnabled(request.enabled());
        config.setRecipients(request.recipients());
        config.setUpdatedAt(Instant.now());
        NotificationConfig saved = configRepository.save(config);
        log.info("Notification config saved: event={} channel={} enabled={}",
                request.eventType(), request.channel(), request.enabled());
        return NotificationConfigDto.from(saved);
    }

    @Transactional
    public void deleteConfig(Long id) {
        NotificationConfig config = configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationConfig", id));
        configRepository.delete(config);
        log.info("Notification config deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDeliveryDto> listDeliveries(Pageable pageable) {
        return PageResponse.from(
                deliveryRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(NotificationDeliveryDto::from));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
