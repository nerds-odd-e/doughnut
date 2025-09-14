package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

public class AssessmentService {
  private final ModelFactoryService modelFactoryService;
  private final TestabilitySettings testabilitySettings;

  public AssessmentService(
      ModelFactoryService modelFactoryService, TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.testabilitySettings = testabilitySettings;
  }

  public AssessmentAttempt generateAssessment(Notebook notebook, User user) {
    List<PredefinedQuestion> questions =
        notebook.getApprovedPredefinedQuestionsForAssessment(
            this.testabilitySettings.getRandomizer());

    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();
    assessmentAttempt.buildQuestions(questions);
    assessmentAttempt.setNotebook(notebook);
    assessmentAttempt.setUser(user);

    return modelFactoryService.save(assessmentAttempt);
  }

  public AssessmentAttempt submitAssessmentResult(
      AssessmentAttempt assessmentAttempt, Timestamp currentUTCTimestamp) {
    int totalCorrectAnswer =
        assessmentAttempt.getAssessmentQuestionInstances().stream()
            .map(AssessmentQuestionInstance::getAnswer)
            .filter(Objects::nonNull)
            .map(Answer::getCorrect)
            .mapToInt(correct -> Boolean.TRUE.equals(correct) ? 1 : 0)
            .sum();
    assessmentAttempt.setAnswersCorrect(totalCorrectAnswer);
    assessmentAttempt.setSubmittedAt(currentUTCTimestamp);

    modelFactoryService.save(assessmentAttempt);

    if (assessmentAttempt.getIsPass() && assessmentAttempt.getNotebook().isCertifiable()) {
      claimCertificateForPassedAssessment(
          assessmentAttempt.getNotebook(), assessmentAttempt.getUser());
    }
    return assessmentAttempt;
  }

  public List<AssessmentAttempt> getMyAssessments(User user) {
    return modelFactoryService.assessmentAttemptRepository.findAllByUser(user);
  }

  private void claimCertificateForPassedAssessment(Notebook notebook, User user) {
    getLastAssessmentAttemptAndItMustBePassed(notebook, user);

    updateExpiry(getOrBuildACertificateFor(notebook, user));
  }

  private Certificate getOrBuildACertificateFor(Notebook notebook, User user) {
    Certificate oldCert =
        modelFactoryService.certificateRepository.findFirstByUserAndNotebook(user, notebook);
    if (oldCert != null) {
      return oldCert;
    }
    Certificate newCert = new Certificate();
    newCert.setUser(user);
    newCert.setNotebook(notebook);
    newCert.setStartDate(this.testabilitySettings.getCurrentUTCTimestamp());
    return newCert;
  }

  private void updateExpiry(Certificate cert) {
    Timestamp expiryDate =
        Timestamp.from(
            ZonedDateTime.ofInstant(
                    this.testabilitySettings.getCurrentUTCTimestamp().toInstant(),
                    ZoneOffset.UTC.normalized())
                .plus(cert.getNotebook().getNotebookSettings().getCertificateExpiry())
                .toInstant());
    cert.setExpiryDate(expiryDate);
    modelFactoryService.save(cert);
  }

  private void getLastAssessmentAttemptAndItMustBePassed(Notebook notebook, User user) {
    getMyAssessments(user).stream()
        .filter(assessmentAttempt -> assessmentAttempt.getNotebook().equals(notebook))
        .reduce((first, second) -> second)
        .filter(AssessmentAttempt::getIsPass)
        .orElseThrow(
            () ->
                new ApiException(
                    "You have not passed the assessment",
                    ASSESSMENT_SERVICE_ERROR,
                    "You have not passed the assessment"));
  }

  public AssessmentQuestionInstance answerQuestion(
      AssessmentQuestionInstance assessmentQuestionInstance, AnswerDTO answerDTO) {
    modelFactoryService.createAnswerForQuestion(assessmentQuestionInstance, answerDTO);
    return assessmentQuestionInstance;
  }
}
