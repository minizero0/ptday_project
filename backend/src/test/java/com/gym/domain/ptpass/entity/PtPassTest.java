package com.gym.domain.ptpass.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.member.entity.Member;
import com.gym.domain.ptpass.exception.InsufficientPtCountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PtPassTest {

    private static final Member MEMBER = new Member("260900001", "김철수", null, null, null);
    private static final int SESSION_MINUTES = 50;

    @Test
    void 부여하면_잔여_횟수가_구매_횟수와_같다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);

        assertThat(ptPass.getTotalCount()).isEqualTo(10);
        assertThat(ptPass.getRemainingCount()).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, PtPass.MAX_PT_COUNT + 1})
    void 구매_횟수는_1회_이상_최대치_이하여야_한다(int totalCount) {
        assertThatThrownBy(() -> new PtPass(MEMBER, SESSION_MINUTES, totalCount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 차감하면_잔여_횟수만_줄고_구매_횟수는_그대로다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);

        ptPass.deduct(1);

        assertThat(ptPass.getRemainingCount()).isEqualTo(9);
        assertThat(ptPass.getTotalCount()).isEqualTo(10);
    }

    @Test
    void 마지막_한_회까지_차감할_수_있다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 1);

        ptPass.deduct(1);

        assertThat(ptPass.getRemainingCount()).isZero();
    }

    @Test
    void 잔여_횟수보다_많이_차감하면_예외를_던지고_횟수는_그대로다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 2);

        assertThatThrownBy(() -> ptPass.deduct(3))
                .isInstanceOf(InsufficientPtCountException.class)
                .extracting(e -> ((InsufficientPtCountException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_PT_COUNT);
        assertThat(ptPass.getRemainingCount()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void 차감_횟수는_1회_이상이어야_한다(int count) {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);

        assertThatThrownBy(() -> ptPass.deduct(count)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 더하는_조정은_잔여_횟수만_늘린다() {
        // 10회권에 서비스 2회: 잔여 12, 구매 10 (늘어난 근거는 조정 이력에 남는다)
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);

        ptPass.adjust(2);

        assertThat(ptPass.getRemainingCount()).isEqualTo(12);
        assertThat(ptPass.getTotalCount()).isEqualTo(10);
    }

    @Test
    void 빼는_조정은_잔여_횟수를_줄인다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);

        ptPass.adjust(-3);

        assertThat(ptPass.getRemainingCount()).isEqualTo(7);
    }

    @Test
    void 잔여_횟수가_음수가_되는_조정은_예외를_던지고_횟수는_그대로다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 2);

        assertThatThrownBy(() -> ptPass.adjust(-3)).isInstanceOf(InsufficientPtCountException.class);
        assertThat(ptPass.getRemainingCount()).isEqualTo(2);
    }

    @Test
    void 증감이_0인_조정은_의미가_없어_거절한다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);

        assertThatThrownBy(() -> ptPass.adjust(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 조정으로_잔여_횟수가_최대치를_넘을_수_없다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, PtPass.MAX_PT_COUNT);

        // 현재 잔여 횟수에 따라 달라지는 실패라 요청 검증으로는 못 거른다. 사용자에게 안내되는 업무 규칙 위반이다.
        assertThatThrownBy(() -> ptPass.adjust(1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PT_COUNT_LIMIT_EXCEEDED);
        assertThat(ptPass.getRemainingCount()).isEqualTo(PtPass.MAX_PT_COUNT);
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 40, 50, 60})
    void 수업_길이는_네_가지_중_하나다(int sessionMinutes) {
        assertThat(new PtPass(MEMBER, sessionMinutes, 10).getSessionMinutes()).isEqualTo(sessionMinutes);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 20, 45, 90})
    void 판매하지_않는_수업_길이로는_만들_수_없다(int sessionMinutes) {
        assertThatThrownBy(() -> new PtPass(MEMBER, sessionMinutes, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 예약_취소로_복원하면_잔여_횟수가_돌아온다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 10);
        ptPass.deduct(1);

        ptPass.restore(1);

        assertThat(ptPass.getRemainingCount()).isEqualTo(10);
    }

    @Test
    void 잔여가_있는지_물어볼_수_있다() {
        PtPass ptPass = new PtPass(MEMBER, SESSION_MINUTES, 1);
        assertThat(ptPass.hasRemaining()).isTrue();

        ptPass.deduct(1);

        assertThat(ptPass.hasRemaining()).isFalse();
    }
}
