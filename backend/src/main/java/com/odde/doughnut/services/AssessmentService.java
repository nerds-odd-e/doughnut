package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;

import com.odde.doughnut.controllers.dto.AnswerDTO;
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
    Randomizer randomizer = this.testabilitySettings.getRandomizer();
    List<Note> notes = randomizer.shuffle(notebook.getNotes());

    List<PredefinedQuestion> questions =
        notes.stream()
            .flatMap(
                note ->
                    randomizer
                        .chooseOneRandomly(
                            note.getPredefinedQuestions().stream()
                                .filter(PredefinedQuestion::isApproved)
                                .toList())
                        .stream())
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

    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();

    questions.stream()
        .limit(numberOfQuestion)
        .map(modelFactoryService::createReviewQuestion)
        .forEach(
            reviewQuestionInstance -> {
              AssessmentQuestionInstance assessmentQuestionInstance =
                  new AssessmentQuestionInstance();
              assessmentQuestionInstance.setAssessmentAttempt(assessmentAttempt);
              assessmentQuestionInstance.setReviewQuestionInstance(reviewQuestionInstance);
              assessmentAttempt.getAssessmentQuestionInstances().add(assessmentQuestionInstance);
            });
    assessmentAttempt.setNotebook(notebook);
    assessmentAttempt.setUser(user);
    modelFactoryService.save(assessmentAttempt);
    return assessmentAttempt;
  }

  public AssessmentResult submitAssessmentResult(
      AssessmentAttempt assessmentAttempt, Timestamp currentUTCTimestamp) {
    assessmentAttempt.setAnswersTotal(assessmentAttempt.getAssessmentQuestionInstances().size());
    assessmentAttempt.setSubmittedAt(currentUTCTimestamp);

    int totalCorrectAnswer =
        assessmentAttempt.getAssessmentQuestionInstances().stream()
            .map(AssessmentQuestionInstance::getReviewQuestionInstance)
            .map(ReviewQuestionInstance::getAnswer)
            .filter(Objects::nonNull)
            .map(Answer::getCorrect)
            .mapToInt(correct -> correct ? 1 : 0)
            .sum();
    assessmentAttempt.setAnswersCorrect(totalCorrectAnswer);

    modelFactoryService.save(assessmentAttempt);

    if (assessmentAttempt.getIsPass() && assessmentAttempt.getNotebook().isCertifiable()) {
      claimCertificateForPassedAssessment(
          assessmentAttempt.getNotebook(), assessmentAttempt.getUser());
    }
    return assessmentAttempt.getAssessmentResult();
  }

  public List<AssessmentAttempt> getMyAssessments(User user) {
    return modelFactoryService.assessmentAttemptRepository.findAllByUser(user);
  }

  private void claimCertificateForPassedAssessment(Notebook notebook, User user) {
    getLastAssessmentAttemptAndItMustBePassed(notebook, user);

    Certificate old_cert =
        modelFactoryService.certificateRepository.findFirstByUserAndNotebook(user, notebook);
    if (old_cert != null) {
      updateExpiry(old_cert);
      return;
    }

    Certificate certificate = new Certificate();
    certificate.setUser(user);
    certificate.setNotebook(notebook);
    certificate.setStartDate(this.testabilitySettings.getCurrentUTCTimestamp());
    updateExpiry(certificate);
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
        .reduce((_, second) -> second)
        .filter(AssessmentAttempt::getIsPass)
        .orElseThrow(
            () ->
                new ApiException(
                    "You have not passed the assessment",
                    ASSESSMENT_SERVICE_ERROR,
                    "You have not passed the assessment"));
  }

  public AnsweredQuestion answerQuestion(
      AssessmentQuestionInstance assessmentQuestionInstance, AnswerDTO answerDTO, User user) {
    return modelFactoryService
        .createAnswerForQuestion(
            assessmentQuestionInstance.getReviewQuestionInstance(),
            answerDTO,
            user,
            testabilitySettings.getCurrentUTCTimestamp())
        .getAnswerViewedByUser(user);
  }
}
