package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.EntityWithId;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record QuizQuestionDirector(
    ReviewPoint reviewPoint,
    QuestionType questionType,
    Randomizer randomizer,
    ModelFactoryService modelFactoryService) {

  public Optional<QuizQuestion> buildQuizQuestion() {
    QuizQuestionFactory quizQuestionFactory = buildQuizQuestionFactory();

    if (!quizQuestionFactory.isValidQuestion()) return Optional.empty();

    QuizQuestion quizQuestion = reviewPoint.createAQuizQuestionOfType(questionType);

    if (quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory) {
      List<EntityWithId> optionsEntities = optionsFactory.getOptionEntities();
      if (optionsEntities.size() <= 1) {
        return Optional.empty();
      }
      quizQuestion.setOptionNoteIds(toIdsString(optionsEntities));
    }

    if (quizQuestionFactory instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setViceReviewPoints(secondaryReviewPointsFactory.getViceReviewPoints());
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }

    return Optional.of(quizQuestion);
  }

  private QuizQuestionFactory buildQuizQuestionFactory() {
    return questionType.factory.apply(
        reviewPoint, new QuizQuestionServant(randomizer, modelFactoryService));
  }

  private String toIdsString(List<EntityWithId> options1) {
    return randomizer.shuffle(options1).stream()
        .map(EntityWithId::getId)
        .map(Object::toString)
        .collect(Collectors.joining(","));
  }
}
