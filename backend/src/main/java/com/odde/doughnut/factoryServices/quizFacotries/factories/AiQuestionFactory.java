package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory {
  private Note note;
  AiQuestionGenerator aiQuestionGenerator;

  public AiQuestionFactory(Note note, AiQuestionGenerator questionGenerator) {
    this.note = note;
    this.aiQuestionGenerator = questionGenerator;
  }

  public QuizQuestionEntity create() {
    QuizQuestionAIQuestion quizQuestionAIQuestion = new QuizQuestionAIQuestion();
    quizQuestionAIQuestion.setNote(note);
    MCQWithAnswer MCQWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note);
    if (MCQWithAnswer == null) {
      return null;
    }
    quizQuestionAIQuestion.setRawJsonQuestion(MCQWithAnswer.toJsonString());
    return quizQuestionAIQuestion;
  }
}
