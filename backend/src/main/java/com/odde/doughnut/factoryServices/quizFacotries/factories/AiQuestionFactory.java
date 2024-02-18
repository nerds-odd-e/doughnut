package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory {
  private Note note;
  private final String questionGenerationModelName;
  private final AiAdvisorService aiAdvisorService;

  public AiQuestionFactory(
      Note note, String questionGenerationModelName, AiAdvisorService aiAdvisorService) {
    this.note = note;
    this.questionGenerationModelName = questionGenerationModelName;
    this.aiAdvisorService = aiAdvisorService;
  }

  public QuizQuestionEntity create() {
    QuizQuestionAIQuestion quizQuestionAIQuestion = new QuizQuestionAIQuestion();
    quizQuestionAIQuestion.setNote(note);
    MCQWithAnswer MCQWithAnswer =
        aiAdvisorService.generateQuestion(note, questionGenerationModelName);
    if (MCQWithAnswer == null) {
      return null;
    }
    quizQuestionAIQuestion.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    return quizQuestionAIQuestion;
  }
}
