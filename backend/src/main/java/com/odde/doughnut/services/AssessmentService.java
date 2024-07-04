package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.controllers.dto.NoteIdAndTitle;
import com.odde.doughnut.controllers.dto.QuestionAnswerPair;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import java.util.Objects;

public class AssessmentService {
  private final QuizQuestionService quizQuestionService;
  private final RealRandomizer realRandomizer = new RealRandomizer();

  public AssessmentService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this.quizQuestionService = new QuizQuestionService(openAiApi, modelFactoryService);
  }

  public List<QuizQuestion> generateAssessment(Notebook notebook) {
    List<Note> notes = realRandomizer.shuffle(notebook.getNotes());

    List<QuizQuestionAndAnswer> questions =
        notes.stream()
            .map(quizQuestionService::selectQuizQuestionForANote)
            .filter(Objects::nonNull)
            .filter(QuizQuestionAndAnswer::isApproved)
            .toList();

    Integer numberOfQuestion = notebook.getNotebookSettings().getNumberOfQuestionsInAssessment();
    if (numberOfQuestion == null || numberOfQuestion == 0) {
      throw new ApiException(
          "The assessment is not available",
          ASSESSMENT_SERVICE_ERROR,
          "The assessment is not available");
    }

    if (questions.size() < numberOfQuestion) {
      throw new ApiException(
          "Not enough questions", ASSESSMENT_SERVICE_ERROR, "Not enough questions");
    }

    return questions.stream()
        .limit(numberOfQuestion)
        .map(QuizQuestionAndAnswer::getQuizQuestion)
        .toList();
  }

  public AssessmentResult submitAssessmentResult(
      Notebook notebook, List<QuestionAnswerPair> questionsAnswerPairs) {
    AssessmentResult assessmentResult = new AssessmentResult();
    assessmentResult.setTotalCount(1);
    assessmentResult.setCorrectCount(0);
    NoteIdAndTitle noteIdAndTitle = new NoteIdAndTitle();
    noteIdAndTitle.setId(2);
    noteIdAndTitle.setTitle("Singapore");
    assessmentResult.setNoteIdAndTitles(new NoteIdAndTitle[] {noteIdAndTitle});
    return assessmentResult;
  }
}
