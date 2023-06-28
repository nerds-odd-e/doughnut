package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.Thingy;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record QuizQuestionDirector(
    ReviewPoint reviewPoint,
    QuestionType questionType,
    Randomizer randomizer,
    ModelFactoryService modelFactoryService,
    AiAdvisorService aiAdvisorService) {

  public Optional<QuizQuestion> buildQuizQuestion() {
    QuizQuestionFactory quizQuestionFactory = buildQuizQuestionFactory();

    try {
      quizQuestionFactory.validatePossibility();

      QuizQuestion quizQuestion = reviewPoint.createAQuizQuestionOfType(questionType);

      quizQuestionFactory.fillQuizQuestion(quizQuestion, aiAdvisorService);

      if (quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory) {
        List<Thingy> optionsEntities = optionsFactory.getOptionEntities();
        if (optionsEntities.size() < optionsFactory.minimumOptionCount()) {
          return Optional.empty();
        }
        quizQuestion.setOptionThingIds(toThingIdsString(optionsEntities));
      }

      if (quizQuestionFactory
          instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
        quizQuestion.setViceReviewPoints(secondaryReviewPointsFactory.getViceReviewPoints());
        quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
      }

      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuizQuestionFactory buildQuizQuestionFactory() {
    return questionType.factory.apply(
        reviewPoint, new QuizQuestionServant(randomizer, modelFactoryService));
  }

  private String toThingIdsString(List<Thingy> options) {
    return randomizer.shuffle(options).stream()
        .map(Thingy::getThing)
        .map(Thing::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
