package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory implements QuizQuestionFactory, QuestionRawJsonFactory {
  private Thing thing;
  private QuizQuestionServant servant;

  public AiQuestionFactory(Thing thing, QuizQuestionServant servant) {
    this.thing = thing;
    this.servant = servant;
  }

  @Override
  public void generateRawJsonQuestion(QuizQuestionEntity quizQuestion)
      throws QuizQuestionNotPossibleException {
    MCQWithAnswer MCQWithAnswer =
        servant.aiAdvisorService.generateQuestion(
            thing.getNote(),
            servant.getGobalSettingsService().getGlobalSettingQuestionGeneration().getValue());
    quizQuestion.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    quizQuestion.setCorrectAnswerIndex(MCQWithAnswer.correctChoiceIndex);
  }
}
