package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
  @Column(name = "choice_index")
  @Nullable
  Integer choiceIndex;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "quiz_question_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  @JsonIgnore
  QuizQuestionEntity question;

  @Column(name = "created_at")
  @Getter
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  public AnswerViewedByUser getViewedByUser(User user, ModelFactoryService modelFactoryService) {
    AnswerViewedByUser answerResult = new AnswerViewedByUser();
    answerResult.answerId = getId();
    answerResult.correct = isCorrect();
    answerResult.answerDisplay = getAnswerDisplay(modelFactoryService);
    answerResult.reviewPoint = getQuestion().getReviewPoint();
    QuizQuestionEntity quizQuestion = getQuestion();
    answerResult.quizQuestion = modelFactoryService.toQuizQuestion(quizQuestion, user);
    return answerResult;
  }

  @JsonIgnore
  private boolean isCorrect() {
    if (question.getQuestionType() == QuizQuestionEntity.QuestionType.SPELLING) {
      return question.getReviewPoint().getNote().matchAnswer(getSpellingAnswer());
    }
    if (question.getQuestionType() == QuizQuestionEntity.QuestionType.JUST_REVIEW) {
      return Objects.equals(getSpellingAnswer(), "yes");
    }
    return Objects.equals(getChoiceIndex(), question.getCorrectAnswerIndex());
  }

  @JsonIgnore
  private String getAnswerDisplay(ModelFactoryService modelFactoryService) {
    if (question != null && choiceIndex != null) {
      return question
          .buildPresenter()
          .getOptions(modelFactoryService)
          .get(choiceIndex)
          .getDisplay();
    }
    return getSpellingAnswer();
  }
}
