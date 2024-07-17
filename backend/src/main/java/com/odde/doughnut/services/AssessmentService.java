package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.controllers.dto.QuestionAnswerPair;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

public class AssessmentService {
  private final ModelFactoryService modelFactoryService;
  private final QuizQuestionService quizQuestionService;
  private final TestabilitySettings testabilitySettings;

  public AssessmentService(
      OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
    this.quizQuestionService = new QuizQuestionService(openAiApi, modelFactoryService);
  }

  public List<QuizQuestion> generateAssessment(Notebook notebook) {
    List<Note> notes = testabilitySettings.getRandomizer().shuffle(notebook.getNotes());

    List<QuizQuestionAndAnswer> questions =
        notes.stream()
            .map(quizQuestionService::selectRandomQuestionForANote)
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
      User user, Notebook notebook, List<QuestionAnswerPair> questionsAnswerPairs) {

    AssessmentAttemptHistory assessmentAttemptHistory = new AssessmentAttemptHistory();
    assessmentAttemptHistory.setUser(user);
    assessmentAttemptHistory.setNotebook(notebook);
    assessmentAttemptHistory.setAnswersCorrect(0); // edit
    assessmentAttemptHistory.setAnswersTotal(questionsAnswerPairs.size());
    assessmentAttemptHistory.setSubmittedAt(new Timestamp(System.currentTimeMillis()));
    modelFactoryService.save(assessmentAttemptHistory);

    AssessmentResult assessmentResult = new AssessmentResult();
    assessmentResult.setTotalCount(questionsAnswerPairs.size());
    assessmentResult.setCorrectCount(0);
    return assessmentResult;
  }
}
