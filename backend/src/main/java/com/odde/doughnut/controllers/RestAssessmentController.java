package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerSubmission;
import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssessmentService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
class RestAssessmentController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final UserModel currentUser;
  private final AssessmentService assessmentService;

  public RestAssessmentController(
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      UserModel currentUser) {
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
    this.assessmentService = new AssessmentService(modelFactoryService, testabilitySettings);
  }

  @GetMapping("/questions/{notebook}")
  public AssessmentAttempt generateAssessmentQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);

    List<QuizQuestion> quizQuestions = assessmentService.generateAssessment(notebook);
    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();
    assessmentAttempt.setNotebook(notebook);
    assessmentAttempt.setQuizQuestions(quizQuestions);

    return assessmentAttempt;
  }

  @PostMapping("{notebook}")
  @Transactional
  public AssessmentResult submitAssessmentResult(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody List<AnswerSubmission> answerSubmissions)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);
    return assessmentService.submitAssessmentResult(
        currentUser.getEntity(),
        notebook,
        answerSubmissions,
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping
  public List<AssessmentAttempt> getMyAssessments() {
    currentUser.assertLoggedIn();
    return assessmentService.getMyAssessments(currentUser.getEntity());
  }
}
