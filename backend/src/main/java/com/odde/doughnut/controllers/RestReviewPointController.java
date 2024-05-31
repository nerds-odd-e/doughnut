package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.QuizQuestion;
import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
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
  public QuizQuestion generateRandomQuestion(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint) {
    currentUser.assertLoggedIn();
    Randomizer randomizer = testabilitySettings.getRandomizer();
    QuizQuestionGenerator quizQuestionGenerator =
        new QuizQuestionGenerator(
            reviewPoint.getUser(), reviewPoint.getNote(), randomizer, modelFactoryService);
    QuizQuestionEntity quizQuestionEntity =
        quizQuestionGenerator.generateAQuestionOfRandomType(questionGenerator);
    return quizQuestionEntity.getQuizQuestion();
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
    if (reviewPoint == null || reviewPoint.getId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The review point does not exist.");
    }
    currentUser.assertLoggedIn();
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
