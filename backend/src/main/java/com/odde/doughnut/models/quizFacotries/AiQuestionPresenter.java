package com.odde.doughnut.models.quizFacotries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;

public class AiQuestionPresenter implements QuizQuestionPresenter {
  private final AIGeneratedQuestion aiQuestion;

  public AiQuestionPresenter(QuizQuestionEntity quizQuestion) {
    try {
      this.aiQuestion =
          new ObjectMapper()
              .readValue(quizQuestion.getRawJsonQuestion(), AIGeneratedQuestion.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String instruction() {
    return aiQuestion.stem;
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public List<QuizQuestion.Option> getOptions(ModelFactoryService modelFactoryService) {

    return List.of();
  }

  @Override
  public boolean isAnswerCorrect(Answer answer) {
    return aiQuestion.correctChoice.equals(answer.getSpellingAnswer());
  }
}
