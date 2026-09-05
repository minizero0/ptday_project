package com.gym.domain.attendance.repository;

import com.gym.domain.attendance.entity.Attendance;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * 시각 범위(반개구간 [start, end))의 출석 목록을 최신순으로 조회한다.
     * 그리드에 회원 번호/이름을 함께 내려주므로 N+1 방지를 위해 member 를 fetch join 한다.
     */
    @Query("""
            select a from Attendance a
            join fetch a.member
            where a.checkedInAt >= :start and a.checkedInAt < :end
            order by a.checkedInAt desc
            """)
    List<Attendance> findAllWithMemberBetween(@Param("start") Instant start, @Param("end") Instant end);
}
