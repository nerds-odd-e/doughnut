package com.odde.doughnut.entities.quizQuestions;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;

@Entity
@DiscriminatorValue("11")
@JsonPropertyOrder({"id", "stem", "options", "correctAnswerIndex", "mainTopic", "imageWithMask"})
public class QuizQuestionAIQuestion extends QuizQuestionEntity {

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
}
