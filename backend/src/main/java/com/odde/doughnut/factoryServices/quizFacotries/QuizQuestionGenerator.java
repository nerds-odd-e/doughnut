package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record QuizQuestionGenerator(
    User user,
    Note note,
    Randomizer randomizer,
    ModelFactoryService modelFactoryService,
    AiAdvisorService aiAdvisorService) {

  private Optional<QuizQuestionEntity> getQuizQuestionEntity(
      QuizQuestionFactory quizQuestionFactory) {
    QuizQuestionServant servant = new QuizQuestionServant(user, randomizer, modelFactoryService);
    try {
      QuizQuestionEntity quizQuestion = quizQuestionFactory.buildQuizQuestion(servant);
      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuizQuestionEntity generateAQuestionOfFirstPossibleType(
      List<QuizQuestionFactory> quizQuestionFactoryStream) {
    QuizQuestionEntity quizQuestionEntity =
        quizQuestionFactoryStream.stream()
            .map(this::getQuizQuestionEntity)
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    modelFactoryService.save(quizQuestionEntity);
    return quizQuestionEntity;
  }

  public QuizQuestionEntity generateAQuestionOfRandomType() {

    List<QuizQuestionFactory> shuffled;
    if (note instanceof HierarchicalNote && user.getAiQuestionTypeOnlyForReview()) {
      AiQuestionFactory aiQuestionFactory = new AiQuestionFactory(note, aiAdvisorService);
      shuffled = List.of(aiQuestionFactory);
    } else {
      shuffled = randomizer.shuffle(note.getQuizQuestionFactories());
    }
    return generateAQuestionOfFirstPossibleType(shuffled);
  }
}
