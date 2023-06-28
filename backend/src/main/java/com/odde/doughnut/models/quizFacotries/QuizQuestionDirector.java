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
    Randomizer randomizer,
    ModelFactoryService modelFactoryService,
    AiAdvisorService aiAdvisorService) {

  public Optional<QuizQuestion> buildQuizQuestion(QuestionType questionType) {
    try {
      return Optional.of(buildRandomQuestion(questionType));
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuizQuestion buildRandomQuestion(QuestionType questionType)
      throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory =
        questionType.factory.apply(
            reviewPoint,
            new QuizQuestionServant(randomizer, modelFactoryService, aiAdvisorService));
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

  private String toThingIdsString(List<Thingy> options) {
    return randomizer.shuffle(options).stream()
        .map(Thingy::getThing)
        .map(Thing::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }

  public QuizQuestion buildRandomQuestion(Boolean aiQuestionTypeOnlyForReview) {
    return this.randomizer
        .shuffle(reviewPoint.availableQuestionTypes(aiQuestionTypeOnlyForReview))
        .stream()
        .map(type -> buildQuizQuestion(type))
        .flatMap(Optional::stream)
        .findFirst()
        .orElseGet(() -> reviewPoint.createAQuizQuestionOfType(QuestionType.JUST_REVIEW));
  }
}
