package com.gym.domain.ptreservation.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.util.BusinessTime;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.ptpass.entity.PtPass;
import com.gym.domain.ptpass.entity.PtPassAdjustment;
import com.gym.domain.ptpass.repository.PtPassAdjustmentRepository;
import com.gym.domain.ptpass.repository.PtPassRepository;
import com.gym.domain.ptreservation.dto.PtReservationCreateRequest;
import com.gym.domain.ptreservation.dto.PtReservationResponse;
import com.gym.domain.ptreservation.entity.PtReservation;
import com.gym.domain.ptreservation.repository.PtReservationRepository;
import com.gym.domain.trainer.entity.Trainer;
import com.gym.domain.trainer.repository.TrainerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PtReservationService {

    private static final Logger log = LoggerFactory.getLogger(PtReservationService.class);

    private static final int RESERVATION_COUNT = 1;
    private static final int MAX_QUERY_DAYS = 31;
    private static final String TRAINER_TIME_CONSTRAINT = "ex_pt_reservation_trainer_time";
    private static final String MEMBER_TIME_CONSTRAINT = "ex_pt_reservation_member_time";
    private static final DateTimeFormatter REASON_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM.dd HH:mm").withZone(BusinessTime.ZONE);

    private final PtReservationRepository ptReservationRepository;
    private final PtPassRepository ptPassRepository;
    private final PtPassAdjustmentRepository ptPassAdjustmentRepository;
    private final MemberRepository memberRepository;
    private final TrainerRepository trainerRepository;
    private final Clock clock;

    public PtReservationService(
            PtReservationRepository ptReservationRepository,
            PtPassRepository ptPassRepository,
            PtPassAdjustmentRepository ptPassAdjustmentRepository,
            MemberRepository memberRepository,
            TrainerRepository trainerRepository,
            Clock clock) {
        this.ptReservationRepository = ptReservationRepository;
        this.ptPassRepository = ptPassRepository;
        this.ptPassAdjustmentRepository = ptPassAdjustmentRepository;
        this.memberRepository = memberRepository;
        this.trainerRepository = trainerRepository;
        this.clock = clock;
    }

    /**
     * 예약 생성. 예약 저장·PT권 1회 차감·차감 이력은 한 트랜잭션이다 (CLAUDE.md §7.1).
     * 트레이너 행을 먼저 잠가 같은 트레이너에 대한 예약을 줄 세운 뒤 겹침을 확인한다.
     * 회원 쪽 겹침은 서로 다른 트레이너 요청이 동시에 올 수 있어 잠금만으로는 다 막지 못하므로,
     * 마지막 방어선은 DB 의 exclusion 제약이다.
     *
     * @param createdBy 처리한 계정 아이디. 요청 본문이 아니라 로그인 정보에서 온 값이어야 한다.
     */
    @Transactional
    public PtReservationResponse create(PtReservationCreateRequest request, String createdBy) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(request.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Trainer trainer = trainerRepository.findByIdForUpdate(request.trainerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINER_NOT_FOUND));

        Instant startTime = request.startTime();
        Instant endTime = startTime.plus(Duration.ofMinutes(request.sessionMinutes()));
        validateNoOverlap(request.trainerId(), request.memberId(), startTime, endTime);

        PtPass ptPass = lockOldestReservablePass(request.memberId(), request.sessionMinutes());

        PtReservation reservation = PtReservation.reserve(ptPass, trainer, startTime, createdBy, clock.instant());
        PtReservation saved = saveOrTranslateConflict(reservation);

        ptPass.deduct(RESERVATION_COUNT);
        ptPassAdjustmentRepository.save(
                PtPassAdjustment.reservation(ptPass, describe("PT 예약", saved), createdBy));

        log.info("PT 예약: reservationId={}, memberId={}, trainerId={}, ptPassId={}, by={}",
                saved.getId(), member.getId(), trainer.getId(), ptPass.getId(), createdBy);
        return PtReservationResponse.from(saved);
    }

    /**
     * 예약 취소. 시점과 관계없이 항상 1회를 돌려준다(운영 정책). 복원과 이력도 한 트랜잭션이다.
     * 예약 행을 먼저 잠그고 상태부터 확인한다 — 동시에 두 번 취소돼도 복원은 한 번뿐이어야 한다.
     */
    @Transactional
    public PtReservationResponse cancel(Long reservationId, String canceledBy) {
        PtReservation reservation = ptReservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PT_RESERVATION_NOT_FOUND));
        reservation.cancel(clock.instant());

        PtPass ptPass = ptPassRepository.findByIdForUpdate(reservation.getPtPass().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PT_PASS_NOT_FOUND));
        ptPass.restore(RESERVATION_COUNT);
        ptPassAdjustmentRepository.save(
                PtPassAdjustment.reservationCancel(ptPass, describe("PT 예약 취소", reservation), canceledBy));

        log.info("PT 예약 취소: reservationId={}, ptPassId={}, by={}", reservationId, ptPass.getId(), canceledBy);
        return PtReservationResponse.from(reservation);
    }

    /** 시간표 조회. 날짜는 영업 시간대(한국) 기준이고 from·to 모두 포함한다. */
    @Transactional(readOnly = true)
    public List<PtReservationResponse> getReservations(Long trainerId, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_QUERY_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_PT_RESERVATION_PERIOD);
        }
        if (!trainerRepository.existsById(trainerId)) {
            throw new BusinessException(ErrorCode.TRAINER_NOT_FOUND);
        }

        Instant fromInstant = from.atStartOfDay(BusinessTime.ZONE).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        return ptReservationRepository.findReservedByTrainerBetween(trainerId, fromInstant, toInstant).stream()
                .map(PtReservationResponse::from)
                .toList();
    }

    private void validateNoOverlap(Long trainerId, Long memberId, Instant startTime, Instant endTime) {
        if (ptReservationRepository.existsTrainerOverlap(trainerId, startTime, endTime)) {
            throw new BusinessException(ErrorCode.TRAINER_TIME_CONFLICT);
        }
        if (ptReservationRepository.existsMemberOverlap(memberId, startTime, endTime)) {
            throw new BusinessException(ErrorCode.MEMBER_TIME_CONFLICT);
        }
    }

    // 먼저 산 순서대로 한 건씩 잠근다. 후보를 읽은 뒤 잠그기 전에 다른 예약이 그 PT권을 다 썼을 수 있어
    // 잠근 다음 잔여를 다시 확인한다. 항상 같은 순서로 잠그므로 요청끼리 서로를 기다리며 멈추지 않는다.
    private PtPass lockOldestReservablePass(Long memberId, int sessionMinutes) {
        return ptPassRepository.findReservableIds(memberId, sessionMinutes).stream()
                .map(ptPassRepository::findByIdForUpdate)
                .flatMap(Optional::stream)
                .filter(PtPass::hasRemaining)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_PT_PASS_FOR_SESSION));
    }

    // 바로 flush 해서 DB 제약 위반을 이 자리에서 받는다 — 커밋 시점에 터지면 500 으로 나간다
    private PtReservation saveOrTranslateConflict(PtReservation reservation) {
        try {
            return ptReservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            String detail = String.valueOf(e.getMostSpecificCause().getMessage());
            if (detail.contains(TRAINER_TIME_CONSTRAINT)) {
                throw new BusinessException(ErrorCode.TRAINER_TIME_CONFLICT);
            }
            if (detail.contains(MEMBER_TIME_CONSTRAINT)) {
                throw new BusinessException(ErrorCode.MEMBER_TIME_CONFLICT);
            }
            // 시간 겹침이 아닌 제약 위반은 우리가 모르는 문제다 — 겹침으로 둔갑시키지 않고 그대로 올린다
            throw e;
        }
    }

    // PT권 변동 이력에 남는 사유. 이력만 봐도 어느 예약 때문인지 알 수 있게 시각과 트레이너를 적는다.
    private String describe(String action, PtReservation reservation) {
        return "%s %s (%s)".formatted(
                action, REASON_TIME_FORMAT.format(reservation.getStartTime()), reservation.getTrainer().getName());
    }
}
