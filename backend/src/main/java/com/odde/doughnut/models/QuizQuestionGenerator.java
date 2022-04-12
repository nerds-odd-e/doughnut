package com.odde.doughnut.models;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import java.util.List;

public record QuizQuestionGenerator(
    ReviewPoint reviewPoint, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  QuizQuestion generateQuestion() {
    List<QuizQuestion.QuestionType> questionTypes = reviewPoint.availableQuestionTypes();
    randomizer.shuffle(questionTypes);
    for (QuizQuestion.QuestionType type : questionTypes) {
      QuizQuestion quizQuestion = buildQuestionOfType(type);
      if (quizQuestion != null) return quizQuestion;
    }
    return buildQuestionOfType(QuizQuestion.QuestionType.JUST_REVIEW);
  }

  private QuizQuestion buildQuestionOfType(QuizQuestion.QuestionType type) {
    QuizQuestionDirector quizQuestionDirector =
        new QuizQuestionDirector(type, randomizer, reviewPoint, modelFactoryService);
    return quizQuestionDirector.buildQuizQuestion();
  }
}
