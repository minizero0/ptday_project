package com.gym.domain.ptreservation.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.util.BusinessTime;
import com.gym.domain.member.entity.Member;
import com.gym.domain.ptpass.entity.PtPass;
import com.gym.domain.trainer.entity.Trainer;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PtReservationTest {

    private static final Instant NOW = seoul(2026, 9, 21, 9, 0);

    private final Member member = new Member("M0001", "홍길동", null, null, null);
    private final Trainer trainer = new Trainer("김코치", null);

    private static Instant seoul(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(BusinessTime.ZONE).toInstant();
    }

    private PtReservation reserve(int sessionMinutes, Instant startTime) {
        return PtReservation.reserve(new PtPass(member, sessionMinutes, 10), trainer, startTime, "desk1", NOW);
    }

    @Test
    void 종료_시각은_시작_시각에_PT권의_수업_길이를_더한_값이다() {
        PtReservation reservation = reserve(50, seoul(2026, 9, 21, 14, 10));

        assertThat(reservation.getEndTime()).isEqualTo(seoul(2026, 9, 21, 15, 0));
        assertThat(reservation.getStatus()).isEqualTo(PtReservationStatus.RESERVED);
        assertThat(reservation.getMember()).isSameAs(member);
        assertThat(reservation.getCreatedBy()).isEqualTo("desk1");
    }

    @Test
    void 시작_시각은_10분_단위여야_한다() {
        assertThatThrownBy(() -> reserve(30, seoul(2026, 9, 21, 14, 5)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_RESERVATION_TIME);
    }

    @Test
    void 초_단위가_섞인_시각은_받지_않는다() {
        assertThatThrownBy(() -> reserve(30, seoul(2026, 9, 21, 14, 10).plusSeconds(30)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_RESERVATION_TIME);
    }

    @Test
    void 영업_시작_전에는_예약할_수_없다() {
        assertThatThrownBy(() -> reserve(30, seoul(2026, 9, 22, 5, 50)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_RESERVATION_TIME);
    }

    @Test
    void 영업_종료_시각에_딱_맞게_끝나는_예약은_된다() {
        PtReservation reservation = reserve(60, seoul(2026, 9, 21, 22, 0));

        assertThat(reservation.getEndTime()).isEqualTo(seoul(2026, 9, 21, 23, 0));
    }

    @Test
    void 영업_종료를_넘겨_끝나는_예약은_할_수_없다() {
        assertThatThrownBy(() -> reserve(60, seoul(2026, 9, 21, 22, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_RESERVATION_TIME);
    }

    @Test
    void 지난_시각에는_예약할_수_없다() {
        assertThatThrownBy(() -> reserve(30, seoul(2026, 9, 21, 8, 50)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_RESERVATION_IN_PAST);
    }

    @Test
    void 비활성_트레이너에게는_예약할_수_없다() {
        trainer.changeActive(false);

        assertThatThrownBy(() -> reserve(30, seoul(2026, 9, 21, 14, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRAINER_INACTIVE);
    }

    @Test
    void 취소하면_상태와_취소_시각이_남는다() {
        PtReservation reservation = reserve(30, seoul(2026, 9, 21, 14, 0));
        Instant canceledAt = seoul(2026, 9, 21, 10, 0);

        reservation.cancel(canceledAt);

        assertThat(reservation.getStatus()).isEqualTo(PtReservationStatus.CANCELED);
        assertThat(reservation.getCanceledAt()).isEqualTo(canceledAt);
    }

    @Test
    void 이미_취소된_예약은_다시_취소할_수_없다() {
        PtReservation reservation = reserve(30, seoul(2026, 9, 21, 14, 0));
        reservation.cancel(NOW);

        assertThatThrownBy(() -> reservation.cancel(NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_RESERVATION_ALREADY_CANCELED);
    }
}
