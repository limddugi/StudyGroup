package io.lcalmsky.app.modules.event.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.*;

import javax.persistence.*;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@ToString
@NamedEntityGraph(
        name = "Enrollment.withEventAndStudy",
        attributeNodes = {
                @NamedAttributeNode(value = "event", subgraph = "study")
        },
        subgraphs = @NamedSubgraph(name = "study", attributeNodes = @NamedAttributeNode("study"))
)
public class Enrollment {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private Account account;

    private LocalDateTime enrolledAt;

    private boolean accepted;

    private boolean attended;



    public static Enrollment of(LocalDateTime enrolledAt, boolean isAbleToAcceptWaitingEnrollment, Account account) {
        Enrollment enrollment = new Enrollment();
        enrollment.enrolledAt = enrolledAt;
        enrollment.accepted = isAbleToAcceptWaitingEnrollment;
        enrollment.account = account;
        return enrollment;
    }

    public void attach(Event event) {
        this.event = event;
    }

    public void detachEvent() {
        this.event = null;
    }

    public void accept() {
        this.accepted = true;
    }

    public void attend() {
        this.attended = true;
    }

    public void absent() {
        this.attended = false;
    }

    public void reject() {
        this.accepted = false;
    }

}
