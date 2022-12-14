package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

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
  @Nullable
  String spellingAnswer;

  @Getter
  @Setter
  @Column(name = "answer_note_id")
  @Nullable
  Integer answerNoteId;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "quiz_question_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  QuizQuestion question;

  @Column(name = "created_at")
  @Getter
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());
}
