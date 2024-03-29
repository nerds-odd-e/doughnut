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

public record QuizQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  private Optional<QuizQuestionEntity> getQuizQuestionEntity(
      QuizQuestionFactory quizQuestionFactory, QuizQuestionServant servant) {
    try {
      QuizQuestionEntity quizQuestion = quizQuestionFactory.buildQuizQuestion(servant);
      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuizQuestionEntity generateAQuestionOfFirstPossibleType(
      List<QuizQuestionFactory> quizQuestionFactoryStream, QuizQuestionServant servant) {
    return quizQuestionFactoryStream.stream()
        .map(quizQuestionFactory -> getQuizQuestionEntity(quizQuestionFactory, servant))
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  public QuizQuestionEntity generateAQuestionOfRandomType(AiQuestionGenerator questionGenerator) {
    QuizQuestionServant servant = new QuizQuestionServant(user, randomizer, modelFactoryService);
    QuizQuestionEntity result;

    List<QuizQuestionFactory> shuffled;
    if (note instanceof HierarchicalNote && user.getAiQuestionTypeOnlyForReview()) {
      shuffled = List.of(new AiQuestionFactory(note, questionGenerator));
    } else {
      shuffled = randomizer.shuffle(note.getQuizQuestionFactories());
    }
    result = generateAQuestionOfFirstPossibleType(shuffled, servant);
    if (result == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated");
    }

    modelFactoryService.save(result);
    return result;
  }
}
