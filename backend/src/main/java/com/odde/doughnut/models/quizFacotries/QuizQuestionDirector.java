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
    try {
      return Optional.of(getQuizQuestion());
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuizQuestion getQuizQuestion() throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory = buildQuizQuestionFactory();
    quizQuestionFactory.validatePossibility();

    QuizQuestion quizQuestion = reviewPoint.createAQuizQuestionOfType(questionType);

    if (quizQuestionFactory instanceof QuestionRawJsonFactory rawJsonFactory) {
      quizQuestion.setRawJsonQuestion(rawJsonFactory.generateRawJsonQuestion());
    }

    if (quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory) {
      List<Thingy> optionsEntities = optionsFactory.getOptionEntities();
      if (optionsEntities.size() < optionsFactory.minimumOptionCount()) {
        throw new QuizQuestionNotPossibleException();
      }
      quizQuestion.setOptionThingIds(toThingIdsString(optionsEntities));
    }

    if (quizQuestionFactory instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setViceReviewPoints(secondaryReviewPointsFactory.getViceReviewPoints());
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }

  private QuizQuestionFactory buildQuizQuestionFactory() {
    return questionType.factory.apply(
        reviewPoint, new QuizQuestionServant(randomizer, modelFactoryService, aiAdvisorService));
  }

  private String toThingIdsString(List<Thingy> options) {
    return randomizer.shuffle(options).stream()
        .map(Thingy::getThing)
        .map(Thing::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
