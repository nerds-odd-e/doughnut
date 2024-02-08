package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionSpelling;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "quiz_answer")
public class Answer extends EntityIdentifiedByIdOnly {
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

  @ManyToOne
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
  public boolean isCorrect() {
      return checkAnswer(this);
  }

  private boolean checkAnswer(Answer answer) {
    if (question instanceof QuizQuestionSpelling) {
      return question.getNote().matchAnswer(answer.getSpellingAnswer());
    }
    return Objects.equals(answer.getChoiceIndex(), question.getCorrectAnswerIndex());
  }

  @JsonIgnore
  public String getAnswerDisplay(ModelFactoryService modelFactoryService) {
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
