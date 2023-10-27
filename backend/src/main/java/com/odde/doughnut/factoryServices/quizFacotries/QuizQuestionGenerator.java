package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.QuizQuestionEntity.QuestionType;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.Optional;

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
}
