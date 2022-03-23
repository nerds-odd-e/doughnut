package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "quiz_answer")
public class Answer {
    @Id
    @Getter
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @Getter
    @Setter
    @Column(name="answer")
    String answer;

    @Getter
    @Setter
    @Column(name="answer_note_id")
    Integer answerNoteId;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "quiz_question_id", referencedColumnName = "id")
    @Getter
    @Setter
    QuizQuestion question;

    @Column(name = "created_at")
    @Getter
    @Setter
    @JsonIgnore
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

}
