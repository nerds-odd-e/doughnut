package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerSubmission;
import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.AssessmentAttemptLimitException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssessmentService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assessment")
class RestAssessmentController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final UserModel currentUser;
  private final AssessmentService assessmentService;

  public RestAssessmentController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      TestabilitySettings testabilitySettings,
      UserModel currentUser) {
    this.testabilitySettings = testabilitySettings;
    this.currentUser = currentUser;
    this.assessmentService =
        new AssessmentService(openAiApi, modelFactoryService, testabilitySettings);
  }

  @GetMapping("/questions/{notebook}")
  public List<QuizQuestion> generateAssessmentQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);
    try {
      currentUser.assertAssessmentAttempt(notebook);
    } catch (AssessmentAttemptLimitException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }

    return assessmentService.generateAssessment(notebook);
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
}
