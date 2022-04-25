package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import java.util.Optional;

public class QuizQuestionDirector {
  private final QuizQuestion.QuestionType questionType;
  private final Randomizer randomizer;
  private final ReviewPoint reviewPoint;
  private final ModelFactoryService modelFactoryService;
  private final QuizQuestionFactory quizQuestionFactory;

  public QuizQuestionDirector(
      ReviewPoint reviewPoint,
      QuestionType questionType,
      Randomizer randomizer,
      ModelFactoryService modelFactoryService) {
    this.questionType = questionType;
    this.randomizer = randomizer;
    this.reviewPoint = reviewPoint;
    this.modelFactoryService = modelFactoryService;
    QuizQuestionServant servant = new QuizQuestionServant(randomizer, modelFactoryService);
    this.quizQuestionFactory = questionType.factory.apply(reviewPoint, servant);
  }

  public Optional<QuizQuestion> buildQuizQuestion1() {
    if (!quizQuestionFactory.isValidQuestion()) return Optional.empty();
    QuizQuestion quizQuestion = QuizQuestion.createAQuizQuestionOfType(reviewPoint, questionType);

    if (!(quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory)) {
      return Optional.of(quizQuestion);
    }

    final String options = optionsFactory.generateOptions(randomizer);
    quizQuestion.setViceReviewPoints(
        optionsFactory.getViceReviewPoints(modelFactoryService.toUserModel(reviewPoint.getUser())));
    quizQuestion.setCategoryLink(optionsFactory.getCategoryLink());
    if (options == null) return Optional.empty();
    quizQuestion.setOptionNoteIds(options);

    return Optional.of(quizQuestion);
  }
}
