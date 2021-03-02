package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
}


