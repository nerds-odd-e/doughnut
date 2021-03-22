package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SpacedRepetition;
import com.odde.doughnut.models.TimestampOperations;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "review_point")
public class ReviewPointEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Override
    public String toString() {
        return "ReviewPoint{" +
                "id=" + id +
                '}';
    }

    @ManyToOne
    @JoinColumn(name = "note_id")
    @JsonIgnore
    @Getter
    @Setter
    private NoteEntity noteEntity;

    @ManyToOne
    @JoinColumn(name = "link_id")
    @JsonIgnore
    @Getter
    @Setter
    private LinkEntity linkEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private UserEntity userEntity;

    @Column(name = "last_reviewed_at")
    @Getter
    @Setter
    private Timestamp lastReviewedAt;

    @Column(name = "next_review_at")
    @Getter
    @Setter
    private Timestamp nextReviewAt;

    @Column(name = "initial_reviewed_at")
    @Getter
    @Setter
    private Timestamp initialReviewedAt;

    @Column(name = "forgetting_curve_index")
    @Getter
    @Setter
    private Integer forgettingCurveIndex = SpacedRepetition.DEFAULT_FORGETTING_CURVE_INDEX;

    @Column(name = "removed_from_review")
    @Getter
    @Setter
    private Boolean removedFromReview = false;

    @Column(name = "note_id", insertable = false, updatable = false)
    @Getter
    private Integer noteId;

    @Column(name = "user_id", insertable = false, updatable = false)
    @Getter
    private Integer userId;

    @Transient
    @Getter
    @Setter
    private Boolean repeatAgainToday = false;

    public boolean isInitialReviewOnSameDay(Timestamp currentTime, ZoneId timeZone) {
        return getDayId(getInitialReviewedAt(), timeZone) == getDayId(currentTime, timeZone);
    }

    public static int getDayId(Timestamp timestamp, ZoneId timeZone) {
        ZonedDateTime systemLocalDateTime = TimestampOperations.getSystemLocalDateTime(timestamp);
        ZonedDateTime userLocalDateTime = systemLocalDateTime.withZoneSameInstant(timeZone);
        return userLocalDateTime.getYear() * 366 + userLocalDateTime.getDayOfYear();
    }

}


