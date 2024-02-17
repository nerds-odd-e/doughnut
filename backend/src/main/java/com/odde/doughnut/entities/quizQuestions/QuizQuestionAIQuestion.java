package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.AiQuestionPresenter;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("11")
public class QuizQuestionAIQuestion extends QuizQuestionEntity {
  @Column(name = "raw_json_question")
  @Getter
  @Setter
  private String rawJsonQuestion;

  @JsonIgnore
  public MCQWithAnswer getMcqWithAnswer() {
    try {
      return new ObjectMapper().readValue(getRawJsonQuestion(), MCQWithAnswer.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonIgnore
  public QuizQuestionPresenter buildPresenter() {
    return new AiQuestionPresenter(this);
  }

  @Override
  public boolean checkAnswer(Answer answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex1());
  }

  @Override
  public Integer getCorrectAnswerIndex1() {
    return getMcqWithAnswer().correctChoiceIndex;
  }
}
