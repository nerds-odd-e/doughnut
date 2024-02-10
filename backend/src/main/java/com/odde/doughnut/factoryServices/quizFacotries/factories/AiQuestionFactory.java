package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory implements QuizQuestionFactory, QuestionRawJsonFactory {
  private Note note;
  private QuizQuestionServant servant;

  public AiQuestionFactory(Note note, QuizQuestionServant servant) {
    this.note = note;
    this.servant = servant;
  }

  @Override
  public void generateRawJsonQuestion(QuizQuestionEntity quizQuestion)
      throws QuizQuestionNotPossibleException {
    MCQWithAnswer MCQWithAnswer =
        servant.aiAdvisorService.generateQuestion(
            note,
            servant.getGlobalSettingsService().getGlobalSettingQuestionGeneration().getValue());
    quizQuestion.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    quizQuestion.setCorrectAnswerIndex(MCQWithAnswer.correctChoiceIndex);
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionAIQuestion();
  }
}
