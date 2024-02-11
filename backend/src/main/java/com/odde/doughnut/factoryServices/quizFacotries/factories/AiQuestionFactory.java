package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory implements QuizQuestionFactory {
  private Note note;

  public AiQuestionFactory(Note note) {
    this.note = note;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    QuizQuestionAIQuestion quizQuestionAIQuestion = new QuizQuestionAIQuestion();
    MCQWithAnswer MCQWithAnswer =
        servant.aiAdvisorService.generateQuestion(
            note,
            servant.getGlobalSettingsService().getGlobalSettingQuestionGeneration().getValue());
    quizQuestionAIQuestion.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    quizQuestionAIQuestion.setCorrectAnswerIndex(MCQWithAnswer.correctChoiceIndex);
    return quizQuestionAIQuestion;
  }
}
