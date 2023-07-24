package com.odde.doughnut.factoryServices.quizFacotries.implementation;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

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
