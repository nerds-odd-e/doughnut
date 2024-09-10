package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.odde.doughnut.controllers.dto.AnswerSubmission;
import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class AssessmentService {
  private final ModelFactoryService modelFactoryService;
  private final TestabilitySettings testabilitySettings;

  public AssessmentService(
      ModelFactoryService modelFactoryService, TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
  }

  public List<QuizQuestion> generateAssessment(Notebook notebook) {
    Randomizer randomizer = this.testabilitySettings.getRandomizer();
    List<Note> notes = randomizer.shuffle(notebook.getNotes());

    List<QuizQuestionAndAnswer> questions =
        notes.stream()
            .flatMap(
                note ->
                    randomizer
                        .chooseOneRandomly(
                            note.getQuizQuestionAndAnswers().stream()
                                .filter(QuizQuestionAndAnswer::isApproved)
                                .toList())
                        .stream())
            .filter(
                (quizQuestionAndAnswer) ->
                    notebook.getLastApprovalTime() == null
                        || quizQuestionAndAnswer
                            .getCreatedAt()
                            .before(notebook.getLastApprovalTime()))
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
      User user,
      Notebook notebook,
      List<AnswerSubmission> answerSubmission,
      Timestamp currentUTCTimestamp) {
    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();
    assessmentAttempt.setUser(user);
    assessmentAttempt.setNotebook(notebook);
    assessmentAttempt.setAnswersTotal(answerSubmission.size());
    assessmentAttempt.setSubmittedAt(currentUTCTimestamp);

    int totalCorrectAnswer =
        (int) answerSubmission.stream().filter(AnswerSubmission::isCorrectAnswers).count();
    assessmentAttempt.setAnswersCorrect(totalCorrectAnswer);

    modelFactoryService.save(assessmentAttempt);
    return assessmentAttempt.getAssessmentResult();
  }

  public List<AssessmentAttempt> getMyAssessments(User user) {
    return modelFactoryService.assessmentAttemptRepository.findAllByUser(user);
  }

  public Certificate claimCertificateForPassedAssessment(Notebook notebook, User user) {
    getLastAssessmentAttemptAndItMustBePassed(notebook, user);

    Certificate old_cert =
        modelFactoryService.certificateRepository.findFirstByUserAndNotebook(user, notebook);
    if (old_cert != null) {
      return updateExpiry(old_cert);
    }

    Certificate certificate = new Certificate();
    certificate.setUser(user);
    certificate.setNotebook(notebook);
    certificate.setStartDate(this.testabilitySettings.getCurrentUTCTimestamp());
    return updateExpiry(certificate);
  }

  private Certificate updateExpiry(Certificate cert) {
    Timestamp expiryDate =
        Timestamp.from(
            ZonedDateTime.ofInstant(
                    this.testabilitySettings.getCurrentUTCTimestamp().toInstant(),
                    ZoneOffset.UTC.normalized())
                .plus(cert.getNotebook().getNotebookSettings().getCertificateExpiry())
                .toInstant());
    cert.setExpiryDate(expiryDate);
    modelFactoryService.save(cert);
    return cert;
  }

  private void getLastAssessmentAttemptAndItMustBePassed(Notebook notebook, User user) {
    getMyAssessments(user).stream()
        .filter(assessmentAttempt -> assessmentAttempt.getNotebook().equals(notebook))
        .reduce((_, second) -> second)
        .filter(AssessmentAttempt::getIsPass)
        .orElseThrow(
            () ->
                new ApiException(
                    "You have not passed the assessment",
                    ASSESSMENT_SERVICE_ERROR,
                    "You have not passed the assessment"));
  }
}
