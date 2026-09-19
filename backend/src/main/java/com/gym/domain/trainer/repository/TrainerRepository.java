package com.gym.domain.trainer.repository;

import com.gym.domain.trainer.entity.Trainer;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainerRepository extends JpaRepository<Trainer, Long> {

    // 시간표의 트레이너 선택용: 재직 중인 사람만
    List<Trainer> findByActiveTrueOrderByNameAscIdAsc();

    // 관리 화면용: 비활성까지. 재직 중인 사람이 위로 온다
    List<Trainer> findAllByOrderByActiveDescNameAscIdAsc();

    /**
     * 예약을 넣기 위한 조회. 같은 트레이너에 대한 예약 요청을 한 번에 하나씩만 처리하도록 행을 잠근다.
     * 잠그지 않으면 두 요청이 동시에 "겹치는 예약 없음"을 확인하고 둘 다 저장을 시도한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trainer t where t.id = :id")
    Optional<Trainer> findByIdForUpdate(@Param("id") Long id);
}
