package com.odde.doughnut.entities;

import java.sql.Timestamp;
import javax.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "marked_questions")
public class MarkedQuestion {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id")
  @Getter
  @Setter
  @NonNull
  private Integer userId;

  @ManyToOne
  @JoinColumn(name = "note_id")
  @Getter
  @Setter
  @NonNull
  private Note note;

  @ManyToOne
  @JoinColumn(name = "quiz_question_id")
  @Getter
  @Setter
  @NonNull
  private QuizQuestionEntity quizQuestion;

  @Column(name = "comment")
  @Getter
  @Setter
  private String comment;

  @Column(name = "is_good")
  @Getter
  @Setter
  @NonNull
  private Boolean isGood = true;

  @Column(name = "created_at")
  @Getter
  @Setter
  @NonNull
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());
}
