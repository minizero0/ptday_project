package com.gym.domain.account.repository;

import com.gym.domain.account.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    // 로그인 시 username 으로 계정 조회
    Optional<Account> findByUsername(String username);

    // 계정 생성/시딩 시 중복 검사용
    boolean existsByUsername(String username);
}
