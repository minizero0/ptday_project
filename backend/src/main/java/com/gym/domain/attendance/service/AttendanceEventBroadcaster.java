package com.gym.domain.attendance.service;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 출석이 기록되면 연결해 둔 데스크 화면들에 바로 알린다(SSE).
 * 출석은 입구 키오스크라는 다른 기기에서 찍히므로, 데스크 화면이 스스로 알 방법이 없기 때문이다.
 * 알림에는 "어느 날짜의 현황이 바뀌었다"만 담고, 목록은 화면이 기존 조회 API 로 다시 가져간다.
 */
@Component
public class AttendanceEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(AttendanceEventBroadcaster.class);

    private static final String CHECK_IN_EVENT_NAME = "check-in";
    // 연결을 무한정 붙잡지 않는다. 시간이 차면 끊고, 화면이 알아서 다시 연결한다.
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitters.remove(emitter));

        // 첫 바이트를 바로 보내 응답 헤더를 내보낸다 — 화면이 "연결됨"을 알고 그 사이 놓친 변경을 다시 불러올 수 있게
        send(emitter, SseEmitter.event().comment("connected"));
        return emitter;
    }

    /**
     * 커밋된 뒤에만 알린다. 커밋 전에 알리면 화면이 다시 조회했을 때 아직 새 출석이 보이지 않을 수 있다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckedIn(AttendanceCheckedInEvent event) {
        emitters.forEach(emitter -> send(
                emitter, SseEmitter.event().name(CHECK_IN_EVENT_NAME).data(event.date().toString())));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // 화면을 닫았거나 네트워크가 끊긴 연결. 출석 처리와 무관하므로 조용히 정리한다.
            emitters.remove(emitter);
            log.debug("끊긴 출석 알림 연결을 정리했습니다: {}", e.getMessage());
        }
    }
}
