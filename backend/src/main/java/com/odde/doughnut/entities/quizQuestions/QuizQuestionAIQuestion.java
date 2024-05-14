package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.ImageWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("11")
@JsonPropertyOrder({"id", "stem", "options", "correctAnswerIndex", "mainTopic", "imageWithMask"})
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

  @Override
  public boolean checkAnswer(Answer answer) {
    return Objects.equals(answer.getChoiceIndex(), getCorrectAnswerIndex());
  }

  public List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    return getMcqWithAnswer().choices.stream()
        .map(
            choice -> {
              QuizQuestion.Choice option = new QuizQuestion.Choice();
              option.setDisplay(choice);
              return option;
            })
        .toList();
  }

  public String getStem() {
    return getMcqWithAnswer().stem;
  }

  public String getMainTopic() {
    return null;
  }

  public ImageWithMask getImageWithMask() {
    return null;
  }

  @Override
  public Integer getCorrectAnswerIndex() {
    return getMcqWithAnswer().correctChoiceIndex;
  }
}
