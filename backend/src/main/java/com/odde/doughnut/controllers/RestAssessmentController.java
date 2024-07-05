package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.controllers.dto.QuestionAnswerPair;
import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssessmentService;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessment")
class RestAssessmentController {
  private final UserModel currentUser;
  private final AssessmentService assessmentService;

  public RestAssessmentController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser) {
    this.currentUser = currentUser;
    this.assessmentService = new AssessmentService(openAiApi, modelFactoryService);
  }

  @GetMapping("/questions/{notebook}")
  public List<QuizQuestion> generateAssessmentQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);

    return assessmentService.generateAssessment(notebook);
  }

  @PostMapping("{notebook}")
  @Transactional
  public AssessmentResult submitAssessmentResult(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody List<QuestionAnswerPair> questionsAnswerPairs)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);

    return assessmentService.submitAssessmentResult(
        currentUser.getEntity(), notebook, questionsAnswerPairs);
  }

  public List<AssessmentAttempt> getAssessmentHistory() {
    return new ArrayList<>();
//    return null;
  }
}
