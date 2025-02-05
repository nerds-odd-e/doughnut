package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingPredefinedFactory;
import com.odde.doughnut.models.Randomizer;
import java.util.List;
import java.util.Optional;

public record PredefinedQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  public PredefinedQuestion generateAQuestionOfRandomType(AiQuestionFactory aiQuestionFactory) {
    List<PredefinedQuestionFactory> factories = getPredefinedQuestionFactories(aiQuestionFactory);
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
      AiQuestionFactory aiQuestionFactory) {
    if (!note.isLink()) {
      return List.of(aiQuestionFactory, new SpellingPredefinedFactory(note));
    }
    return List.of(aiQuestionFactory);
  }
}
