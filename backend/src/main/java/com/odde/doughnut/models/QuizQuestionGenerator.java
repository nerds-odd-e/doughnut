package com.odde.doughnut.models;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import java.util.List;
import java.util.Optional;

public record QuizQuestionGenerator(
    ReviewPoint reviewPoint, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  QuizQuestion generateQuestion() {
    List<QuizQuestion.QuestionType> questionTypes = reviewPoint.availableQuestionTypes();
    randomizer.shuffle(questionTypes);
    return questionTypes.stream()
        .map(this::buildQuestionOfType)
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(buildQuestionOfType(QuizQuestion.QuestionType.JUST_REVIEW).orElse(null));
  }

  private Optional<QuizQuestion> buildQuestionOfType(QuestionType type) {
    QuizQuestionDirector quizQuestionDirector =
        new QuizQuestionDirector(reviewPoint, type, randomizer, modelFactoryService);
    return quizQuestionDirector.buildQuizQuestion1();
  }
}
