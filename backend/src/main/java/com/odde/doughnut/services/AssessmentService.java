package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.theokanning.openai.client.OpenAiApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class AssessmentService {
  private final QuizQuestionService quizQuestionService;

  public AssessmentService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.quizQuestionService = new QuizQuestionService(openAiApi, modelFactoryService);
  }

  public List<QuizQuestion> generateAssessment(Notebook notebook) {

    System.out.println("########## SIZE: " + notebook.getNotes().size());

    List<Note> notes = notebook.getNotes();
    List<QuizQuestion> finalQuizQuestionList = new ArrayList<>();
    for (Note note : notes) {
      List<QuizQuestion> quizQuestionList = quizQuestionService.selectQuizQuestionsForANote(note);
      finalQuizQuestionList.addAll(quizQuestionList);
    }

    List<QuizQuestion> questions = finalQuizQuestionList.stream().filter(QuizQuestion::isApproved).toList();

    if (questions.size() < 5) {
      throw new ApiException(
          "Not enough approved questions",
          ASSESSMENT_SERVICE_ERROR,
          "Not enough approved questions");
    }

    return questions;
  }

  public long countQuizQuestions(Notebook notebook) {
    return notebook.getNotes().stream()
        .map(quizQuestionService::selectQuizQuestionForANote)
        .filter(Objects::nonNull)
        .count();
  }
}
