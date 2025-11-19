package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssessmentService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
class AssessmentController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final UserModel currentUser;
  private final AssessmentService assessmentService;
  private final AuthorizationService authorizationService;

  public AssessmentController(
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      UserModel currentUser,
      AuthorizationService authorizationService) {
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
    this.authorizationService = authorizationService;
    this.assessmentService = new AssessmentService(modelFactoryService, testabilitySettings);
  }

  @PostMapping("/questions/{notebook}")
  @Transactional
  public AssessmentAttempt generateAssessmentQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn(currentUser.getEntity());
    authorizationService.assertReadAuthorization(currentUser.getEntity(), notebook);

    return assessmentService.generateAssessment(notebook, currentUser.getEntity());
  }

  @PostMapping("/{assessmentQuestionInstance}/answer")
  @Transactional
  public AssessmentQuestionInstance answerQuestion(
      @PathVariable("assessmentQuestionInstance") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance,
      @Valid @RequestBody AnswerDTO answerDTO)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn(currentUser.getEntity());
    authorizationService.assertAuthorization(currentUser.getEntity(), assessmentQuestionInstance);

    return assessmentService.answerQuestion(assessmentQuestionInstance, answerDTO);
  }

  @PostMapping("{assessmentAttempt}")
  @Transactional
  public AssessmentAttempt submitAssessmentResult(
      @PathVariable("assessmentAttempt") @Schema(type = "integer")
          AssessmentAttempt assessmentAttempt)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn(currentUser.getEntity());
    authorizationService.assertAuthorization(currentUser.getEntity(), assessmentAttempt);
    return assessmentService.submitAssessmentResult(
        assessmentAttempt, testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping
  public List<AssessmentAttempt> getMyAssessments() {
    authorizationService.assertLoggedIn(currentUser.getEntity());
    return assessmentService.getMyAssessments(currentUser.getEntity());
  }
}
