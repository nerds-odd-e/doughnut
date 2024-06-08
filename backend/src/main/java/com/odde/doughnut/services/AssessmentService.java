package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.theokanning.openai.client.OpenAiApi;

public class AssessmentService {
  private final ModelFactoryService modelFactoryService;

  public AssessmentService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public QuizQuestion getQuizQuestion(Note note) {
    return this.modelFactoryService.getQuizQuestionsByNote(note).stream()
        .filter(q -> q.approved)
        .findFirst()
        .orElse(null);
  }
}
