package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ReviewService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review-questions")
class RestReviewQuestionController {
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final ReviewService reviewService;

  public RestReviewQuestionController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.reviewService =
        new ReviewService(openAiApi, modelFactoryService, testabilitySettings.getRandomizer());
  }

  @PostMapping("/generate-question")
  @Transactional
  public ReviewQuestionInstance generateQuestion(
      @RequestParam(value = "note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    return reviewService.generateAQuestionOfRandomType(note, currentUser.getEntity());
  }

  @GetMapping("/{reviewPoint}/random-question")
  @Transactional
  public ReviewQuestionInstance generateRandomQuestion(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint) {
    currentUser.assertLoggedIn();
    return reviewService.generateAQuestionOfRandomType(
        reviewPoint.getNote(), currentUser.getEntity());
  }

  @PostMapping("/{reviewQuestionInstance}/regenerate")
  @Transactional
  public ReviewQuestionInstance regenerate(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance) {
    currentUser.assertLoggedIn();
    return reviewService.generateAQuestionOfRandomType(
        reviewQuestionInstance.getPredefinedQuestion().getNote(), currentUser.getEntity());
  }

  @PostMapping("/{reviewQuestionInstance}/contest")
  @Transactional
  public ReviewQuestionContestResult contest(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance) {
    currentUser.assertLoggedIn();
    return reviewService.contest(reviewQuestionInstance);
  }

  @PostMapping("/{reviewQuestionInstance}/answer")
  @Transactional
  public AnsweredQuestion answerQuiz(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance,
      @Valid @RequestBody AnswerDTO answerDTO) {
    currentUser.assertLoggedIn();

    return reviewService.answerQuestion(
        reviewQuestionInstance,
        answerDTO,
        currentUser.getEntity(),
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping(path = "/{reviewQuestionInstance}")
  @Transactional
  public AnsweredQuestion showQuestion(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(reviewQuestionInstance);
    return reviewQuestionInstance.getAnsweredQuestion();
  }
}
