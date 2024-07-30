package io.lcalmsky.app.modules.account.infra.predicates;

import com.querydsl.core.types.Predicate;
import io.lcalmsky.app.modules.account.domain.entity.QAccount;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;

import java.util.Set;

public class AccountPredicates {
    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones) {
        QAccount account = QAccount.account;
        return account.zones.any().in(zones).and(account.tags.any().in(tags));
    }
}

// 위에서 작성한 내용은 계정이 가진 지역 관련 정보중 어느 하나라도 전달된 지역 정보에 포함되는지,
// 관심사도 마찬가지인지 확인하는 조건절이다.