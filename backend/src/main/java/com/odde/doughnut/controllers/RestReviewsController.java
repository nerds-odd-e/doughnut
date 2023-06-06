package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.DueReviewPoints;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
class RestReviewsController {
  private final ModelFactoryService modelFactoryService;

  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewsController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public ReviewStatus overview() {
    currentUser.assertLoggedIn();
    return currentUser
        .createReviewing(testabilitySettings.getCurrentUTCTimestamp())
        .getReviewStatus();
  }

  @GetMapping("/initial")
  @Transactional(readOnly = true)
  public List<ReviewPoint> initialReview() {
    currentUser.assertLoggedIn();
    Reviewing reviewing = currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp());

    return reviewing.getDueInitialReviewPoints().collect(Collectors.toList());
  }

  @PostMapping(path = "")
  @Transactional
  public ReviewPoint create(@RequestBody InitialInfo initialInfo) {
    currentUser.assertLoggedIn();
    ReviewPoint reviewPoint =
        ReviewPoint.buildReviewPointForThing(
            modelFactoryService.thingRepository.findById(initialInfo.thingId).orElse(null));
    reviewPoint.setRemovedFromReview(initialInfo.skipReview);

    ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
    reviewPointModel.initialReview(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return reviewPointModel.getEntity();
  }

  @GetMapping(value = {"/repeat"})
  @Transactional
  public DueReviewPoints repeatReview(
      @RequestParam(value = "max", required = false) Integer max,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    currentUser.assertLoggedIn();
    Reviewing reviewing = currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    return reviewing.getDueReviewPoints(max, dueInDays, testabilitySettings.getRandomizer());
  }

  @PostMapping("/answer")
  @Transactional
  public AnswerResult answerQuiz(@Valid @RequestBody Answer answer) {
    currentUser.assertLoggedIn();
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    answerModel.updateReviewPoints(testabilitySettings.getCurrentUTCTimestamp());
    answerModel.save();
    return answerModel.getAnswerResult();
  }

  @GetMapping(path = "/answers/{answer}")
  @Transactional
  public AnswerViewedByUser showAnswer(@PathVariable("answer") Answer answer)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(answer);
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    AnswerViewedByUser answerResult = answerModel.getAnswerViewedByUser();
    answerResult.reviewPoint = answer.getQuestion().getReviewPoint();
    answerResult.quizQuestion =
        new QuizQuestionViewedByUser(
            answer.getQuestion(), modelFactoryService, currentUser.getEntity());
    return answerResult;
  }
}
