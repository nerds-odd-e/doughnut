package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import java.util.List;
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
    return Optional.ofNullable(buildQuizQuestion());
  }

  public QuizQuestion buildQuizQuestion() {
    if (!quizQuestionFactory.isValidQuestion()) return null;
    QuizQuestion quizQuestion = new QuizQuestion();
    quizQuestion.setReviewPoint(reviewPoint);
    quizQuestion.setQuestionType(questionType);
    String options = getOptions();
    if (options == null) return null;
    quizQuestion.setOptionNoteIds(options);
    List<ReviewPoint> viceReviewPoints =
        quizQuestionFactory.getViceReviewPoints(
            modelFactoryService.toUserModel(reviewPoint.getUser()));

    if (quizQuestionFactory.minimumViceReviewPointCount() > viceReviewPoints.size()) {
      return null;
    }
    quizQuestion.setViceReviewPoints(viceReviewPoints);
    quizQuestion.setCategoryLink(quizQuestionFactory.getCategoryLink());
    return quizQuestion;
  }

  private String getOptions() {
    if (quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory) {
      return optionsFactory.generateOptions(randomizer);
    }
    if (quizQuestionFactory instanceof QuestionLinkOptionsFactory linkOptionsFactory) {
      return linkOptionsFactory.generateOptions(randomizer);
    }
    return "";
  }
}
