package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AssessmentService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
class AssessmentController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AssessmentService assessmentService;
  private final AuthorizationService authorizationService;

  @Autowired
  public AssessmentController(
      TestabilitySettings testabilitySettings,
      AssessmentService assessmentService,
      AuthorizationService authorizationService) {
    this.testabilitySettings = testabilitySettings;
    this.assessmentService = assessmentService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/questions/{notebook}")
  @Transactional
  public AssessmentAttempt generateAssessmentQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertReadAuthorization(notebook);
    return assessmentService.generateAssessment(notebook, authorizationService.getCurrentUser());
  }

  @PostMapping("/{assessmentQuestionInstance}/answer")
  @Transactional
  public AssessmentQuestionInstance answerQuestion(
      @PathVariable("assessmentQuestionInstance") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance,
      @Valid @RequestBody AnswerDTO answerDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAuthorization(assessmentQuestionInstance);

    return assessmentService.answerQuestion(assessmentQuestionInstance, answerDTO);
  }

  @PostMapping("{assessmentAttempt}")
  @Transactional
  public AssessmentAttempt submitAssessmentResult(
      @PathVariable("assessmentAttempt") @Schema(type = "integer")
          AssessmentAttempt assessmentAttempt)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAuthorization(assessmentAttempt);
    return assessmentService.submitAssessmentResult(
        assessmentAttempt, testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping
  public List<AssessmentAttempt> getMyAssessments() {
    authorizationService.assertLoggedIn();
    return assessmentService.getMyAssessments(authorizationService.getCurrentUser());
  }
}
