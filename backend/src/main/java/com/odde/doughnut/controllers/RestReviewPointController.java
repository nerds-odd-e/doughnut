package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionGenerator;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/review-points")
class RestReviewPointController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AiQuestionGenerator questionGenerator;

  public RestReviewPointController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    questionGenerator =
        new AiQuestionGenerator(openAiApi, new GlobalSettingsService(modelFactoryService));
  }

  @GetMapping("/{reviewPoint}")
  public ReviewPoint show(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(reviewPoint);
    return reviewPoint;
  }

  @GetMapping("/{reviewPoint}/random-question")
  @Transactional
  public ReviewQuestionInstance generateRandomQuestion(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint) {
    currentUser.assertLoggedIn();
    Randomizer randomizer = testabilitySettings.getRandomizer();
    PredefinedQuestionGenerator predefinedQuestionGenerator =
        new PredefinedQuestionGenerator(
            reviewPoint.getUser(), reviewPoint.getNote(), randomizer, modelFactoryService);
    PredefinedQuestion question =
        predefinedQuestionGenerator.generateAQuestionOfRandomType(questionGenerator);
    return modelFactoryService.createQuizQuestion(question);
  }

  @PostMapping(path = "/{reviewPoint}/remove")
  @Transactional
  public ReviewPoint removeFromRepeating(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint) {
    reviewPoint.setRemovedFromReview(true);
    modelFactoryService.save(reviewPoint);
    return reviewPoint;
  }

  @PostMapping(path = "/{reviewPoint}/self-evaluate")
  @Transactional
  public ReviewPoint selfEvaluate(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint,
      @RequestBody SelfEvaluation selfEvaluation) {
    currentUser.assertLoggedIn();
    if (reviewPoint == null || reviewPoint.getId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The review point does not exist.");
    }
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .updateForgettingCurve(selfEvaluation.adjustment);
    return reviewPoint;
  }

  @PatchMapping(path = "/{reviewPoint}/mark-as-repeated")
  @Transactional
  public ReviewPoint markAsRepeated(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint,
      @RequestParam("successful") boolean successful) {
    currentUser.assertLoggedIn();
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .markAsRepeated(testabilitySettings.getCurrentUTCTimestamp(), successful);
    return reviewPoint;
  }
}
