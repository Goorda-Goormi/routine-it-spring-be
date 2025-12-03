package com.goormi.routine.domain.notification.service;

import com.goormi.routine.domain.notification.dto.NotificationResponse;
import com.goormi.routine.domain.notification.repository.SseEmitterRepository;
import com.goormi.routine.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterServiceImpl implements SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    private final SseEmitterRepository sseEmitterRepository;

    @Override
    public SseEmitter subscribe(Long userId, String lastEmitterId) {
        String emitterId = makeTimeIncludeId(userId);
        SseEmitter emitter = sseEmitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> sseEmitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> sseEmitterRepository.deleteById(emitterId));

        // 503 에러를 방지하고, 최초 연결 시 식별자를 보내기 위한 더미 이벤트 전송
        sendToClient(emitter, emitterId, "EventStream Created. [userId=" + userId + "]");

        // 클라이언트가 미수신한 Event 목록이 존재할 경우 전송하여 Event 유실을 예방
        if (hasLostData(lastEmitterId)) {
            sendLostData(lastEmitterId, userId, emitter);
        }

        return emitter;
    }

    @Override
    public void sendToClient(Long userId, Object data) {
        String eventId = makeTimeIncludeId(userId);
        Map<String, SseEmitter> emitters = sseEmitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(userId));
        emitters.forEach(
                (emitterId, emitter) -> {
                    sseEmitterRepository.saveEventCache(emitterId, data);
                    sendToClient(emitter, emitterId, data);
                }
        );
    }

    @Override
    public void sendNotification(User receiver, NotificationResponse notificationResponse) {
        sendToClient(receiver.getId(), notificationResponse);
    }

    private String makeTimeIncludeId(Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    // emitterId는 Emitter를 식별하는 고유 ID, eventId는 전송되는 이벤트의 식별자 역할을 하지만, 
    // 여기서는 emitterId를 eventId로 사용하여 클라이언트가 마지막 수신한 Emitter를 식별하도록 함
    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(data));
        } catch (IOException exception) {
            sseEmitterRepository.deleteById(emitterId);
            log.error("SSE 연결 오류!", exception);
        }
    }

    private boolean hasLostData(String lastEmitterId) {
        return !lastEmitterId.isEmpty();
    }

    private void sendLostData(String lastEmitterId, Long userId, SseEmitter emitter) {
        Map<String, Object> events = sseEmitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(userId));
        events.entrySet().stream()
                .filter(entry -> lastEmitterId.compareTo(entry.getKey()) < 0)
                .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
    }

    @Scheduled(fixedRate = 10000)
    public void sendHeartbeat() {
        final Map<String, SseEmitter> emitters = sseEmitterRepository.findAllEmitters();
        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                log.debug("Heartbeat sent to emitter: {}", emitterId);
            } catch (IOException e) {
                sseEmitterRepository.deleteById(emitterId);
                log.error("Failed to send heartbeat to emitter: {}", emitterId, e);
            }
        });
    }
}
