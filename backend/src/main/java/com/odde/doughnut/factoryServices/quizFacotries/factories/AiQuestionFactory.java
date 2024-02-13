package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory {
  private Note note;
  private final AiAdvisorService aiAdvisorService;

  public AiQuestionFactory(Note note, AiAdvisorService aiAdvisorService) {
    this.note = note;
    this.aiAdvisorService = aiAdvisorService;
  }

  public QuizQuestionEntity create(QuizQuestionServant servant) {
    QuizQuestionAIQuestion quizQuestionAIQuestion = new QuizQuestionAIQuestion();
    quizQuestionAIQuestion.setNote(note);
    MCQWithAnswer MCQWithAnswer =
        aiAdvisorService.generateQuestion(
            note,
            servant.getGlobalSettingsService().getGlobalSettingQuestionGeneration().getValue());
    if (MCQWithAnswer == null) {
      return null;
    }
    quizQuestionAIQuestion.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    quizQuestionAIQuestion.setCorrectAnswerIndex(MCQWithAnswer.correctChoiceIndex);
    return quizQuestionAIQuestion;
  }
}
