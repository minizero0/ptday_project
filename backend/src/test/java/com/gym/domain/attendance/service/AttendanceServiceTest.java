package com.gym.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.util.BusinessTime;
import com.gym.domain.attendance.dto.AttendanceCheckInRequest;
import com.gym.domain.attendance.dto.CheckInResult;
import com.gym.domain.attendance.dto.CheckInResult.CheckInCandidate;
import com.gym.domain.attendance.entity.Attendance;
import com.gym.domain.attendance.repository.AttendanceRepository;
import com.gym.domain.member.entity.Member;
import com.gym.domain.member.repository.MemberRepository;
import com.gym.domain.membership.entity.Membership;
import com.gym.domain.membership.service.RepresentativeMembershipFinder;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    private static final String LAST_DIGITS = "5678";

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RepresentativeMembershipFinder representativeMembershipFinder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void 뒷자리가_한_명과_일치하고_이용권이_이용중이면_출석이_기록된다() {
        Member member = member("홍길동");
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS)).thenReturn(List.of(member));
        givenMembership(member, today().minusDays(10), today().plusDays(20));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInResult result = attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.KIOSK);

        assertThat(result.status()).isEqualTo(CheckInResult.Status.CHECKED_IN);
        assertThat(result.attendance().name()).isEqualTo("홍길동");
        assertThat(result.attendance().membership().daysRemaining()).isEqualTo(20);
        verify(attendanceRepository).save(any(Attendance.class));
        verify(eventPublisher).publishEvent(new AttendanceCheckedInEvent(today()));
    }

    @Test
    void 만료일_당일까지는_출석할_수_있다() {
        Member member = member("홍길동");
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS)).thenReturn(List.of(member));
        givenMembership(member, today().minusDays(30), today());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInResult result = attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.KIOSK);

        assertThat(result.status()).isEqualTo(CheckInResult.Status.CHECKED_IN);
        assertThat(result.attendance().membership().daysRemaining()).isZero();
    }

    @Test
    void 이용권이_없는_회원은_출석이_거절되고_기록도_남지_않는다() {
        Member member = member("홍길동");
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS)).thenReturn(List.of(member));
        when(representativeMembershipFinder.findByMemberId(any(), any())).thenReturn(Optional.empty());

        assertRejected(
                ErrorCode.MEMBERSHIP_NOT_ACTIVE,
                () -> attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.KIOSK));
    }

    @Test
    void 이용권이_만료된_회원은_만료됐다는_사유로_출석이_거절된다() {
        Member member = member("홍길동");
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS)).thenReturn(List.of(member));
        givenMembership(member, today().minusDays(40), today().minusDays(1));

        assertRejected(
                ErrorCode.MEMBERSHIP_EXPIRED,
                () -> attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.KIOSK));
    }

    @Test
    void 이용권이_아직_시작_전인_회원은_출석이_거절된다() {
        Member member = member("홍길동");
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS)).thenReturn(List.of(member));
        givenMembership(member, today().plusDays(3), today().plusDays(33));

        assertRejected(
                ErrorCode.MEMBERSHIP_NOT_ACTIVE,
                () -> attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.STAFF));
    }

    @Test
    void 뒷자리가_여러_명과_일치하면_키오스크에는_이름을_가린_후보를_내려준다() {
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS))
                .thenReturn(List.of(member("홍길동"), member("남궁민수"), member("이준")));

        CheckInResult result = attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.KIOSK);

        assertThat(result.status()).isEqualTo(CheckInResult.Status.CANDIDATES);
        assertThat(result.candidates()).extracting(CheckInCandidate::name)
                .containsExactly("홍*동", "남**수", "이*");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void 뒷자리가_여러_명과_일치하면_직원_화면에는_이름을_그대로_내려준다() {
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS))
                .thenReturn(List.of(member("홍길동"), member("이준")));

        CheckInResult result = attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.STAFF);

        assertThat(result.candidates()).extracting(CheckInCandidate::name).containsExactly("홍길동", "이준");
    }

    @Test
    void 뒷자리가_네_자리_숫자가_아니면_거절한다() {
        assertThatThrownBy(() -> attendanceService.checkIn(byLastDigits("12a4"), CheckInChannel.KIOSK))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 뒷자리와_일치하는_회원이_없으면_회원_없음으로_거절한다() {
        when(memberRepository.findActiveByPhoneEndingWith(LAST_DIGITS)).thenReturn(List.of());

        assertThatThrownBy(() -> attendanceService.checkIn(byLastDigits(LAST_DIGITS), CheckInChannel.KIOSK))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    private void assertRejected(ErrorCode expected, Runnable checkIn) {
        assertThatThrownBy(checkIn::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
        verify(attendanceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(AttendanceCheckedInEvent.class));
    }

    private void givenMembership(Member member, LocalDate startDate, LocalDate endDate) {
        when(representativeMembershipFinder.findByMemberId(any(), any()))
                .thenReturn(Optional.of(new Membership(member, startDate, endDate)));
    }

    private static AttendanceCheckInRequest byLastDigits(String digits) {
        return new AttendanceCheckInRequest(null, digits, null);
    }

    private static Member member(String name) {
        return new Member("260900001", name, "010-1234-5678", null, null);
    }

    private static LocalDate today() {
        return LocalDate.now(BusinessTime.ZONE);
    }
}
