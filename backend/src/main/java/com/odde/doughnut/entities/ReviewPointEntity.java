package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "initial_reviewed_at")
    @Getter
    @Setter
    private Timestamp initialReviewedAt;

    public boolean isInitialReviewOnSameDay(Timestamp currentTime, ZoneId timeZone) {
        return getYearId(getInitialReviewedAt(), timeZone) == getYearId(currentTime, timeZone);
    }

    public static int getYearId(Timestamp timestamp, ZoneId timeZone) {
        ZonedDateTime systemLocalDateTime = timestamp.toLocalDateTime().atZone(ZoneId.systemDefault());
        ZonedDateTime userLocalDateTime = systemLocalDateTime.withZoneSameInstant(timeZone);
        return userLocalDateTime.getYear() * 366 + userLocalDateTime.getDayOfYear();
    }

}


