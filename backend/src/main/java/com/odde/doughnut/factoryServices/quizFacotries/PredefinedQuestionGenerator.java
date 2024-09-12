package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PredefinedQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  private Optional<PredefinedQuestion> buildOne(QuizQuestionFactory quizQuestionFactory) {
    try {
      return Optional.of(quizQuestionFactory.buildValidQuizQuestion());
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private PredefinedQuestion generateAQuestionOfFirstPossibleType(
      List<QuizQuestionFactory> quizQuestionFactoryStream) {
    return quizQuestionFactoryStream.stream()
        .map(this::buildOne)
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  public PredefinedQuestion generateAQuestionOfRandomType(AiQuestionGenerator questionGenerator) {
    List<QuizQuestionFactory> shuffled;
    if (note instanceof HierarchicalNote && user.getAiQuestionTypeOnlyForReview()) {
      shuffled = List.of(new AiQuestionFactory(note, questionGenerator));
    } else {
      shuffled =
          randomizer.shuffle(
              note.getQuizQuestionFactories(
                  new QuizQuestionServant(user, randomizer, modelFactoryService)));
    }
    PredefinedQuestion result = generateAQuestionOfFirstPossibleType(shuffled);
    if (result == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated");
    }

    modelFactoryService.save(result);
    return result;
  }
}
