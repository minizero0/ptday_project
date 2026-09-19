package com.gym.domain.ptreservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.util.BusinessTime;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.ptpass.entity.PtPass;
import com.gym.domain.ptpass.entity.PtPassAdjustment;
import com.gym.domain.ptpass.entity.PtPassAdjustmentType;
import com.gym.domain.ptpass.repository.PtPassAdjustmentRepository;
import com.gym.domain.ptpass.repository.PtPassRepository;
import com.gym.domain.ptreservation.dto.PtReservationCreateRequest;
import com.gym.domain.ptreservation.dto.PtReservationResponse;
import com.gym.domain.ptreservation.entity.PtReservation;
import com.gym.domain.ptreservation.entity.PtReservationStatus;
import com.gym.domain.ptreservation.repository.PtReservationRepository;
import com.gym.domain.trainer.entity.Trainer;
import com.gym.domain.trainer.repository.TrainerRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PtReservationServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long TRAINER_ID = 3L;
    private static final long RESERVATION_ID = 7L;
    private static final long OLDER_PASS_ID = 11L;
    private static final long NEWER_PASS_ID = 12L;
    private static final String STAFF = "desk1";
    private static final Instant NOW = seoul(2026, 9, 21, 9, 0);
    private static final Instant START = seoul(2026, 9, 21, 14, 10);
    private static final Instant END_50 = seoul(2026, 9, 21, 15, 0);

    @Mock
    private PtReservationRepository ptReservationRepository;
    @Mock
    private PtPassRepository ptPassRepository;
    @Mock
    private PtPassAdjustmentRepository ptPassAdjustmentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TrainerRepository trainerRepository;

    private PtReservationService service;

    private final Member member = new Member("M0001", "홍길동", null, null, null);
    private final Trainer trainer = new Trainer("김코치", null);

    private static Instant seoul(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(BusinessTime.ZONE).toInstant();
    }

    @BeforeEach
    void setUp() {
        service = new PtReservationService(
                ptReservationRepository, ptPassRepository, ptPassAdjustmentRepository,
                memberRepository, trainerRepository, Clock.fixed(NOW, BusinessTime.ZONE));
    }

    private PtReservationCreateRequest request() {
        return new PtReservationCreateRequest(MEMBER_ID, TRAINER_ID, 50, START);
    }

    private void givenMemberAndTrainer() {
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        when(trainerRepository.findByIdForUpdate(TRAINER_ID)).thenReturn(Optional.of(trainer));
    }

    @Test
    void 예약하면_먼저_산_PT권에서_1회를_차감하고_이력을_남긴다() {
        givenMemberAndTrainer();
        PtPass older = new PtPass(member, 50, 10);
        PtPass newer = new PtPass(member, 50, 20);
        when(ptPassRepository.findReservableIds(MEMBER_ID, 50)).thenReturn(List.of(OLDER_PASS_ID, NEWER_PASS_ID));
        when(ptPassRepository.findByIdForUpdate(OLDER_PASS_ID)).thenReturn(Optional.of(older));
        when(ptReservationRepository.saveAndFlush(any(PtReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PtReservationResponse response = service.create(request(), STAFF);

        assertThat(older.getRemainingCount()).isEqualTo(9);
        assertThat(newer.getRemainingCount()).isEqualTo(20);
        verify(ptPassRepository, never()).findByIdForUpdate(NEWER_PASS_ID);
        assertThat(response.endTime()).isEqualTo(END_50);
        assertThat(response.memberName()).isEqualTo("홍길동");
        assertThat(response.trainerName()).isEqualTo("김코치");
        assertThat(response.status()).isEqualTo(PtReservationStatus.RESERVED);

        ArgumentCaptor<PtPassAdjustment> captor = ArgumentCaptor.forClass(PtPassAdjustment.class);
        verify(ptPassAdjustmentRepository).save(captor.capture());
        PtPassAdjustment adjustment = captor.getValue();
        assertThat(adjustment.getType()).isEqualTo(PtPassAdjustmentType.RESERVATION);
        assertThat(adjustment.getDelta()).isEqualTo(-1);
        assertThat(adjustment.getRemainingAfter()).isEqualTo(9);
        assertThat(adjustment.getAdjustedBy()).isEqualTo(STAFF);
        assertThat(adjustment.getReason()).isEqualTo("PT 예약 09.21 14:10 (김코치)");
    }

    @Test
    void 잠그고_보니_먼저_산_PT권이_그사이_소진됐으면_다음_PT권에서_차감한다() {
        givenMemberAndTrainer();
        PtPass exhausted = new PtPass(member, 50, 1);
        exhausted.deduct(1);
        PtPass newer = new PtPass(member, 50, 20);
        when(ptPassRepository.findReservableIds(MEMBER_ID, 50)).thenReturn(List.of(OLDER_PASS_ID, NEWER_PASS_ID));
        when(ptPassRepository.findByIdForUpdate(OLDER_PASS_ID)).thenReturn(Optional.of(exhausted));
        when(ptPassRepository.findByIdForUpdate(NEWER_PASS_ID)).thenReturn(Optional.of(newer));
        when(ptReservationRepository.saveAndFlush(any(PtReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request(), STAFF);

        assertThat(exhausted.getRemainingCount()).isZero();
        assertThat(newer.getRemainingCount()).isEqualTo(19);
    }

    @Test
    void 해당_수업_길이의_잔여_PT권이_없으면_예약할_수_없다() {
        givenMemberAndTrainer();
        when(ptPassRepository.findReservableIds(MEMBER_ID, 50)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NO_PT_PASS_FOR_SESSION);
        verify(ptReservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void 트레이너의_다른_예약과_겹치면_예약할_수_없고_횟수도_그대로다() {
        givenMemberAndTrainer();
        when(ptReservationRepository.existsTrainerOverlap(TRAINER_ID, START, END_50)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRAINER_TIME_CONFLICT);
        verify(ptPassRepository, never()).findReservableIds(any(), any(Integer.class));
    }

    @Test
    void 회원의_다른_예약과_겹치면_예약할_수_없다() {
        givenMemberAndTrainer();
        when(ptReservationRepository.existsMemberOverlap(MEMBER_ID, START, END_50)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_TIME_CONFLICT);
    }

    @Test
    void 동시_요청이_DB_제약에_걸리면_겹침_오류로_바꿔_알린다() {
        givenMemberAndTrainer();
        when(ptPassRepository.findReservableIds(MEMBER_ID, 50)).thenReturn(List.of(OLDER_PASS_ID));
        when(ptPassRepository.findByIdForUpdate(OLDER_PASS_ID)).thenReturn(Optional.of(new PtPass(member, 50, 10)));
        when(ptReservationRepository.saveAndFlush(any(PtReservation.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "conflicting key value violates exclusion constraint \"ex_pt_reservation_member_time\""));

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_TIME_CONFLICT);
        verify(ptPassAdjustmentRepository, never()).save(any());
    }

    @Test
    void 겹침과_무관한_DB_제약_위반은_겹침_오류로_둔갑시키지_않는다() {
        givenMemberAndTrainer();
        when(ptPassRepository.findReservableIds(MEMBER_ID, 50)).thenReturn(List.of(OLDER_PASS_ID));
        when(ptPassRepository.findByIdForUpdate(OLDER_PASS_ID)).thenReturn(Optional.of(new PtPass(member, 50, 10)));
        when(ptReservationRepository.saveAndFlush(any(PtReservation.class)))
                .thenThrow(new DataIntegrityViolationException("violates foreign key constraint"));

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 없는_회원이나_트레이너로는_예약할_수_없다() {
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        when(trainerRepository.findByIdForUpdate(TRAINER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(), STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRAINER_NOT_FOUND);
    }

    @Test
    void 취소하면_차감했던_PT권에_1회를_돌려주고_이력을_남긴다() {
        PtPass ptPass = new PtPass(member, 50, 10);
        ptPass.deduct(1);
        PtReservation reservation = PtReservation.reserve(ptPass, trainer, START, STAFF, NOW);
        when(ptReservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(ptPassRepository.findByIdForUpdate(any())).thenReturn(Optional.of(ptPass));

        PtReservationResponse response = service.cancel(RESERVATION_ID, "admin");

        assertThat(response.status()).isEqualTo(PtReservationStatus.CANCELED);
        assertThat(response.canceledAt()).isEqualTo(NOW);
        assertThat(ptPass.getRemainingCount()).isEqualTo(10);

        ArgumentCaptor<PtPassAdjustment> captor = ArgumentCaptor.forClass(PtPassAdjustment.class);
        verify(ptPassAdjustmentRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PtPassAdjustmentType.RESERVATION_CANCEL);
        assertThat(captor.getValue().getDelta()).isEqualTo(1);
        assertThat(captor.getValue().getAdjustedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getReason()).isEqualTo("PT 예약 취소 09.21 14:10 (김코치)");
    }

    @Test
    void 이미_취소된_예약을_다시_취소해도_횟수는_한_번만_복원된다() {
        PtPass ptPass = new PtPass(member, 50, 10);
        ptPass.deduct(1);
        PtReservation reservation = PtReservation.reserve(ptPass, trainer, START, STAFF, NOW);
        reservation.cancel(NOW);
        when(ptReservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(RESERVATION_ID, STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_RESERVATION_ALREADY_CANCELED);
        assertThat(ptPass.getRemainingCount()).isEqualTo(9);
        verify(ptPassAdjustmentRepository, never()).save(any());
    }

    @Test
    void 없는_예약은_취소할_수_없다() {
        when(ptReservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(RESERVATION_ID, STAFF))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_RESERVATION_NOT_FOUND);
    }

    @Test
    void 시간표는_영업_시간대_기준_날짜_범위를_시각으로_바꿔_조회한다() {
        when(trainerRepository.existsById(TRAINER_ID)).thenReturn(true);
        LocalDate from = LocalDate.of(2026, 9, 21);
        LocalDate to = LocalDate.of(2026, 9, 27);
        when(ptReservationRepository.findReservedByTrainerBetween(
                eq(TRAINER_ID), eq(seoul(2026, 9, 21, 0, 0)), eq(seoul(2026, 9, 28, 0, 0))))
                .thenReturn(List.of(PtReservation.reserve(new PtPass(member, 50, 10), trainer, START, STAFF, NOW)));

        List<PtReservationResponse> responses = service.getReservations(TRAINER_ID, from, to);

        assertThat(responses).extracting(PtReservationResponse::startTime).containsExactly(START);
    }

    @Test
    void 조회_기간이_뒤집혔거나_너무_길면_거부한다() {
        assertThatThrownBy(() ->
                service.getReservations(TRAINER_ID, LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 21)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_RESERVATION_PERIOD);

        assertThatThrownBy(() ->
                service.getReservations(TRAINER_ID, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PT_RESERVATION_PERIOD);
    }
}
