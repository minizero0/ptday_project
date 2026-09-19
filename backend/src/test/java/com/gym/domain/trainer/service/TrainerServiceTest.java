package com.gym.domain.trainer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.domain.trainer.dto.TrainerResponse;
import com.gym.domain.trainer.dto.TrainerSaveRequest;
import com.gym.domain.trainer.entity.Trainer;
import com.gym.domain.trainer.repository.TrainerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    private static final long TRAINER_ID = 3L;

    @Mock
    private TrainerRepository trainerRepository;

    @InjectMocks
    private TrainerService trainerService;

    @Test
    void 트레이너를_등록하면_재직_상태로_시작하고_전화번호_표기를_통일한다() {
        when(trainerRepository.save(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrainerResponse response = trainerService.createTrainer(new TrainerSaveRequest("김코치", "01011112222", null));

        assertThat(response.name()).isEqualTo("김코치");
        assertThat(response.phone()).isEqualTo("010-1111-2222");
        assertThat(response.active()).isTrue();
    }

    @Test
    void 트레이너_정보를_수정하고_비활성_처리할_수_있다() {
        Trainer trainer = new Trainer("김코치", "010-1111-2222");
        when(trainerRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer));

        TrainerResponse response =
                trainerService.updateTrainer(TRAINER_ID, new TrainerSaveRequest("김수석", "", false));

        assertThat(response.name()).isEqualTo("김수석");
        assertThat(response.phone()).isNull();
        assertThat(response.active()).isFalse();
    }

    @Test
    void 수정_요청에_재직_여부가_없으면_그대로_둔다() {
        Trainer trainer = new Trainer("김코치", null);
        when(trainerRepository.findById(TRAINER_ID)).thenReturn(Optional.of(trainer));

        TrainerResponse response =
                trainerService.updateTrainer(TRAINER_ID, new TrainerSaveRequest("김코치", null, null));

        assertThat(response.active()).isTrue();
    }

    @Test
    void 없는_트레이너는_수정할_수_없다() {
        when(trainerRepository.findById(TRAINER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                trainerService.updateTrainer(TRAINER_ID, new TrainerSaveRequest("김코치", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRAINER_NOT_FOUND);
    }

    @Test
    void 기본_목록은_재직_중인_트레이너만_내려준다() {
        when(trainerRepository.findByActiveTrueOrderByNameAscIdAsc())
                .thenReturn(List.of(new Trainer("김코치", null)));

        assertThat(trainerService.getTrainers(false)).extracting(TrainerResponse::name).containsExactly("김코치");
    }

    @Test
    void 관리_화면은_비활성_트레이너까지_볼_수_있다() {
        Trainer inactive = new Trainer("박코치", null);
        inactive.changeActive(false);
        when(trainerRepository.findAllByOrderByActiveDescNameAscIdAsc())
                .thenReturn(List.of(new Trainer("김코치", null), inactive));

        assertThat(trainerService.getTrainers(true)).extracting(TrainerResponse::active).containsExactly(true, false);
    }
}
