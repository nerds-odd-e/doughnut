package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SpellingPredefinedFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record PredefinedQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  public PredefinedQuestion generateAQuestionOfRandomType(AiQuestionGenerator questionGenerator) {
    List<PredefinedQuestionFactory> factories = getPredefinedQuestionFactories(questionGenerator);
    PredefinedQuestion result = generateAQuestionOfFirstPossibleType(randomizer.shuffle(factories));
    if (result == null) {
      return null;
    }
    modelFactoryService.save(result);
    return result;
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
    if (note.getLinkType() == null) {
      return List.of(
          new AiQuestionFactory(note, questionGenerator), new SpellingPredefinedFactory(note));
    }
    PredefinedQuestionServant servant =
        new PredefinedQuestionServant(user, randomizer, modelFactoryService);
    return Arrays.stream(note.getLinkType().getQuestionTypes())
        .map(t -> t.factoryForLinkingNote.apply(note, servant))
        .toList();
  }
}
