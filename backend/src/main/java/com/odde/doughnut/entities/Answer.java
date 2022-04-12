package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_answer")
public class Answer {
  @Id
  @Getter
  @JsonIgnore
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Getter
  @Setter
  @Column(name = "answer")
  String spellingAnswer;

  @Getter
  @Setter
  @Column(name = "answer_note_id")
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
