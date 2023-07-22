package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;

public class AiQuestionFactory implements QuizQuestionFactory, QuestionRawJsonFactory {
  private ReviewPoint reviewPoint;
  private QuizQuestionServant servant;

  public AiQuestionFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.servant = servant;
  }

  @Override
  public void generateRawJsonQuestion(QuizQuestionEntity quizQuestion)
      throws QuizQuestionNotPossibleException {
    AIGeneratedQuestion aiGeneratedQuestion =
        servant.aiAdvisorService.generateQuestion(reviewPoint.getNote());
    quizQuestion.setRawJsonQuestion(aiGeneratedQuestion.toJsonString());
    quizQuestion.setCorrectAnswerIndex(aiGeneratedQuestion.correctChoiceIndex);
  }
}
