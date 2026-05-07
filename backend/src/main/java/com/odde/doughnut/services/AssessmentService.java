package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.AssessmentAttemptRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssessmentService {
  private final AssessmentAttemptRepository assessmentAttemptRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final AnswerService answerService;

  @Autowired
  public AssessmentService(
      AssessmentAttemptRepository assessmentAttemptRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AnswerService answerService) {
    this.assessmentAttemptRepository = assessmentAttemptRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.answerService = answerService;
  }

  public AssessmentAttempt generateAssessment(Notebook notebook, User user) {
    List<PredefinedQuestion> questions =
        notebook.getApprovedPredefinedQuestionsForAssessment(
            this.testabilitySettings.getRandomizer());

    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();
    assessmentAttempt.buildQuestions(questions);
    assessmentAttempt.setNotebook(notebook);
    assessmentAttempt.setUser(user);

    return entityPersister.save(assessmentAttempt);
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

    entityPersister.save(assessmentAttempt);

    return assessmentAttempt;
  }

  public List<AssessmentAttempt> getMyAssessments(User user) {
    return assessmentAttemptRepository.findAllByUser(user);
  }

  public AssessmentQuestionInstance answerQuestion(
      AssessmentQuestionInstance assessmentQuestionInstance, AnswerDTO answerDTO) {
    answerService.createAnswerForQuestion(assessmentQuestionInstance, answerDTO);
    return assessmentQuestionInstance;
  }
}
