package io.lcalmsky.app.modules.event.infra.repository;

import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(value = "Event.withEnrollments", type = EntityGraph.EntityGraphType.FETCH)
    List<Event> findByStudyOrderByStartDateTime(Study study);
}
