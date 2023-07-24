package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;

public class AiQuestionFactory implements QuizQuestionFactory, QuestionRawJsonFactory {
  private Thing thing;
  private QuizQuestionServant servant;

  public AiQuestionFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.thing = reviewPoint.getThing();
    this.servant = servant;
  }

  @Override
  public void generateRawJsonQuestion(QuizQuestionEntity quizQuestion)
      throws QuizQuestionNotPossibleException {
    AIGeneratedQuestion aiGeneratedQuestion =
        servant.aiAdvisorService.generateQuestion(thing.getNote());
    quizQuestion.setRawJsonQuestion(aiGeneratedQuestion.toJsonString());
    quizQuestion.setCorrectAnswerIndex(aiGeneratedQuestion.correctChoiceIndex);
  }
}
