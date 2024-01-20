package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.QuizQuestionEntity.QuestionType;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record QuizQuestionGenerator(
    User user,
    Thing thing,
    Randomizer randomizer,
    ModelFactoryService modelFactoryService,
    AiAdvisorService aiAdvisorService) {

  public Optional<QuizQuestionEntity> buildQuizQuestion(QuestionType questionType) {
    try {
      QuizQuestionServant servant =
          new QuizQuestionServant(user, randomizer, modelFactoryService, aiAdvisorService);
      QuizQuestionEntity quizQuestion =
          new QuizQuestionDirector(questionType, servant).invoke(thing);
      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  public QuizQuestion generateAQuestionOfFirstPossibleType(List<QuestionType> shuffled) {
    var quizQuestionEntity =
        shuffled.stream()
            .map(this::buildQuizQuestion)
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    modelFactoryService.createRecord(quizQuestionEntity);
    return modelFactoryService.toQuizQuestion(quizQuestionEntity, user);
  }
}
