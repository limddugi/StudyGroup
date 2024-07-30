package io.lcalmsky.app.modules.account.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long>, QuerydslPredicateExecutor<Account> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Account findByEmail(String email); // 이메일 회원 정보를 조회할 수 있게 메서드를 추가

    Account findByNickname(String username);

    @EntityGraph(attributePaths = {"tags", "zones"})
    Account findAccountWithTagsAndZonesById(Long id);
}
