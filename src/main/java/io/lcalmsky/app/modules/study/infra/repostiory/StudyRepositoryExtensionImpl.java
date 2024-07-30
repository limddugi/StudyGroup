package io.lcalmsky.app.modules.study.infra.repostiory;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import io.lcalmsky.app.modules.account.domain.entity.QAccount;
import io.lcalmsky.app.modules.account.domain.entity.QZone;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.QStudy;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.QTag;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.Set;

public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {

    public StudyRepositoryExtensionImpl() {
        super(Study.class);
    }

    @Override
    public Page<Study> findByKeyword(String keyword, Pageable pageable) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag).fetchJoin()
                .leftJoin(study.zones, QZone.zone).fetchJoin()
                .leftJoin(study.members, QAccount.account).fetchJoin()
                .distinct();
        JPQLQuery<Study> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        QueryResults<Study> fetchResults = pageableQuery.fetchResults();
        return new PageImpl<>(fetchResults.getResults(), pageable, fetchResults.getTotal());
    }
    // getQuerydsl() 을 이용해 QuerydslRepositorySupport가 제공하는 기능을 사용할 수 있는데 페이징을 적용하기 위해 applyPagination을 호출
    //그리고 fetchResults를 이용해 조회한 결과를 얻을 수 있다.
    //반환해야 할 타입이 Page 이므로 구현체인 PageImpl을 이용해 반환한다.
    //결과 데이터, pageable, 전체 데이터 수를 생성자로 전달해주어야 한다.
    // 마지막으로 MainController 에서 view로 전달해주는 이름이 바뀌었기에 serach.html을 수정해야 한다.

    @Override
    public List<Study> findByAccount(Set<Tag> tags, Set<Zone> zones) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study).where(study.published.isTrue()
                        .and(study.closed.isFalse())
                        .and(study.tags.any().in(tags))
                        .and(study.zones.any().in(zones)))
                .leftJoin(study.tags, QTag.tag).fetchJoin()
                .leftJoin(study.zones, QZone.zone).fetchJoin()
                .orderBy(study.publishedDateTime.desc())
                .distinct()
                .limit(9);
        return query.fetch();
    }
}
