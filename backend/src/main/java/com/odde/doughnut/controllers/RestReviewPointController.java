package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.entities.json.SelfEvaluation;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.OpenAiApi;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/review-points")
class RestReviewPointController {
  private final ModelFactoryService modelFactoryService;
  private final AiAdvisorService aiAdvisorService;
  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewPointController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/{reviewPoint}")
  public ReviewPoint show(@PathVariable("reviewPoint") ReviewPoint reviewPoint)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(reviewPoint);
    return reviewPoint;
  }

  @GetMapping("/{reviewPoint}/random-question")
  @Transactional
  public QuizQuestion generateRandomQuestion(@PathVariable("reviewPoint") ReviewPoint reviewPoint) {
    currentUser.assertLoggedIn();
    User user = currentUser.getEntity();
    QuizQuestionEntity quizQuestionEntity =
        modelFactoryService
            .toReviewPointModel(reviewPoint)
            .generateAQuizQuestion(testabilitySettings.getRandomizer(), user, aiAdvisorService)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    modelFactoryService.quizQuestionRepository.save(quizQuestionEntity);
    return modelFactoryService.toQuizQuestion(quizQuestionEntity, user);
  }

  @PostMapping(path = "/{reviewPoint}/remove")
  public ReviewPoint removeFromRepeating(ReviewPoint reviewPoint) {
    reviewPoint.setRemovedFromReview(true);
    modelFactoryService.reviewPointRepository.save(reviewPoint);
    return reviewPoint;
  }

  @PostMapping(path = "/{reviewPoint}/self-evaluate")
  @Transactional
  public ReviewPoint selfEvaluate(
      ReviewPoint reviewPoint, @RequestBody SelfEvaluation selfEvaluation) {
    if (reviewPoint == null || reviewPoint.getId() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The review point does not exist.");
    }
    currentUser.assertLoggedIn();
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .updateForgettingCurve(selfEvaluation.adjustment);
    return reviewPoint;
  }

  @PostMapping(path = "/{reviewPoint}/mark-as-repeated")
  @Transactional
  public ReviewPoint markAsRepeated(
      ReviewPoint reviewPoint, @RequestParam("successful") boolean successful) {
    currentUser.assertLoggedIn();
    modelFactoryService
        .toReviewPointModel(reviewPoint)
        .markAsRepeated(testabilitySettings.getCurrentUTCTimestamp(), successful);
    return reviewPoint;
  }
}
