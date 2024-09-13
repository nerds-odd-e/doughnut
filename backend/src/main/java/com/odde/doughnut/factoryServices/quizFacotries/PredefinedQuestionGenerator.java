package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import java.util.List;
import java.util.Optional;

public record PredefinedQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  private Optional<PredefinedQuestion> buildOne(
      PredefinedQuestionFactory predefinedQuestionFactory) {
    try {
      return Optional.of(predefinedQuestionFactory.buildValidPredefinedQuestion());
    } catch (PredefinedQuestionNotPossibleException e) {
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

  public PredefinedQuestion generateAQuestionOfRandomType(AiQuestionGenerator questionGenerator) {
    List<PredefinedQuestionFactory> shuffled;
    if (note instanceof HierarchicalNote && user.getAiQuestionTypeOnlyForReview()) {
      shuffled = List.of(new AiQuestionFactory(note, questionGenerator));
    } else {
      shuffled =
          randomizer.shuffle(
              note.getPredefinedQuestionFactories(
                  new PredefinedQuestionServant(user, randomizer, modelFactoryService)));
    }
    PredefinedQuestion result = generateAQuestionOfFirstPossibleType(shuffled);
    if (result == null) {
      return null;
    }

    modelFactoryService.save(result);
    return result;
  }
}
