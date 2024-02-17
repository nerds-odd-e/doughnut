package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.factoryServices.quizFacotries.*;
import com.odde.doughnut.models.UserModel;
import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type", discriminatorType = DiscriminatorType.INTEGER)
public abstract class QuizQuestionEntity extends EntityIdentifiedByIdOnly {

  @ManyToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  public abstract QuizQuestionPresenter buildPresenter();

  @JsonIgnore
  public ReviewPoint getReviewPointFor(UserModel userModel) {
    return userModel.getReviewPointFor(getNote());
  }

  public abstract Integer getCorrectAnswerIndex();

  public abstract boolean checkAnswer(Answer answer);
}
