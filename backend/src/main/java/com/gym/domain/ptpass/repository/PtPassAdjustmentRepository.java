package com.gym.domain.ptpass.repository;

import com.gym.domain.ptpass.entity.PtPassAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PtPassAdjustmentRepository extends JpaRepository<PtPassAdjustment, Long> {

    // 최근 변동이 위로. 같은 시각이면 나중에 기록된 것(id 큰 값)이 먼저다.
    List<PtPassAdjustment> findByPtPassIdOrderByCreatedAtDescIdDesc(Long ptPassId);
}
