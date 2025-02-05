package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingPredefinedFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import java.util.List;
import java.util.Optional;

public record PredefinedQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  public PredefinedQuestion generateAQuestionOfRandomType(AiQuestionGenerator questionGenerator) {
    return generateAQuestionOfRandomType(questionGenerator, null);
  }

  public PredefinedQuestion generateAQuestionOfRandomType(
      AiQuestionGenerator questionGenerator, QuestionContestResult contestResult) {
    List<PredefinedQuestionFactory> factories = getPredefinedQuestionFactories(questionGenerator);
    return generateAQuestionOfFirstPossibleType(randomizer.shuffle(factories));
  }

  private Optional<PredefinedQuestion> buildOne(
      PredefinedQuestionFactory predefinedQuestionFactory) {
    try {
      return Optional.of(predefinedQuestionFactory.buildValidPredefinedQuestion());
    } catch (PredefinedQuestionNotPossibleException | OpenAiUnauthorizedException e) {
      return Optional.empty();
    }
  }

  private PredefinedQuestion generateAQuestionOfFirstPossibleType(
      List<PredefinedQuestionFactory> predefinedQuestionFactoryStream) {
    return predefinedQuestionFactoryStream.stream()
        .map(this::buildOne)
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  private List<PredefinedQuestionFactory> getPredefinedQuestionFactories(
      AiQuestionGenerator questionGenerator) {
    AiQuestionFactory aiQuestionFactory = new AiQuestionFactory(note, questionGenerator);
    if (!note.isLink()) {
      return List.of(aiQuestionFactory, new SpellingPredefinedFactory(note));
    }
    return List.of(aiQuestionFactory);
  }
}
