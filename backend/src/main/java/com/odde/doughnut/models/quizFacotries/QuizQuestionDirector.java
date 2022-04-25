package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.EntityWithId;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    quizQuestion.setViceReviewPoints(
        optionsFactory.getViceReviewPoints(modelFactoryService.toUserModel(reviewPoint.getUser())));

    quizQuestion.setCategoryLink(optionsFactory.getCategoryLink());

    EntityWithId answerNote = optionsFactory.generateAnswer();
    if (answerNote != null) {
      List<? extends EntityWithId> fillingOptions = optionsFactory.generateFillingOptions();
      if (!fillingOptions.isEmpty()) {
        List<EntityWithId> options1 = new ArrayList<>(fillingOptions);
        options1.add(answerNote);
        String options =
            randomizer.shuffle(options1).stream()
                .map(EntityWithId::getId)
                .map(Object::toString)
                .collect(Collectors.joining(","));
        quizQuestion.setOptionNoteIds(options);
        return Optional.of(quizQuestion);
      }
    }
    return Optional.empty();
  }
}
