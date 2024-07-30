package io.lcalmsky.app.modules.study.infra.repostiory;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;
import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension{
    boolean existsByPath(String path);

    @EntityGraph(value = "Study.withAll", type = LOAD)
    Study findByPath(String path);

    @EntityGraph(value = "Study.withTagsAndManagers", type = FETCH)
    Study findStudyWithTagsByPath(String path);

    @EntityGraph(value = "Study.withZonesAndManagers", type = FETCH)
    Study findStudyWithZonesByPath(String path);

    @EntityGraph(value = "Study.withManagers", type= FETCH)
    Study findStudyWithManagersByPath(String path);

    @EntityGraph(value = "Study.withMembers", type = FETCH)
    Study findStudyWithMembersByPath(String path);

    Optional<Study> findStudyOnlyByPath(String path);

    @EntityGraph(value = "Study.withTagsAndZones", type = FETCH)
    Study findStudyWithTagsAndZonesById(Long id);

    @EntityGraph(attributePaths = {"managers", "members"})
    Study findStudyWithManagersAndMembersById(Long id);


    @EntityGraph(attributePaths = {"tags", "zones"})
    List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published, boolean closed);

    List<Study> findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(Account account, boolean closed);

    List<Study> findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(Account account, boolean closed);

    List<Study> findByAccount(Set<Tag> tags, Set<Zone> zones);
}
