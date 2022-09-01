package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.entities.json.RepetitionForUser;
import com.odde.doughnut.entities.json.ReviewStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reviews")
class RestReviewsController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewsController(
      ModelFactoryService modelFactoryService,
      CurrentUserFetcher currentUserFetcher,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public ReviewStatus overview() {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    return user.createReviewing(testabilitySettings.getCurrentUTCTimestamp()).getReviewStatus();
  }

  @GetMapping("/initial")
  @Transactional(readOnly = true)
  public List<ReviewPoint> initialReview() {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());

    return reviewing.getDueInitialReviewPoints().collect(Collectors.toList());
  }

  @PostMapping(path = "")
  @Transactional
  public ReviewPoint create(@RequestBody InitialInfo initialInfo) {
    UserModel userModel = currentUserFetcher.getUser();
    userModel.getAuthorization().assertLoggedIn();
    ReviewPoint reviewPoint = new ReviewPoint();
    reviewPoint.setThing(
        modelFactoryService.thingRepository.findById(initialInfo.thingId).orElse(null));
    reviewPoint.setRemovedFromReview(initialInfo.skipReview);

    ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
    reviewPointModel.initialReview(userModel, testabilitySettings.getCurrentUTCTimestamp());
    return reviewPointModel.getEntity();
  }

  @GetMapping("/repeat")
  @Transactional
  public RepetitionForUser repeatReview() {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    return reviewing
        .getOneRepetitionForUser(testabilitySettings.getRandomizer())
        .orElseThrow(
            () -> {
              throw new ResponseStatusException(
                  HttpStatus.NOT_FOUND, "no more repetition for today");
            });
  }

  @PostMapping("/answer")
  @Transactional
  public AnswerResult answerQuiz(@Valid @RequestBody Answer answer) {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    answerModel.updateReviewPoints(testabilitySettings.getCurrentUTCTimestamp());
    answerModel.save();
    AnswerResult answerResult = answerModel.getAnswerResult();
    Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    answerResult.nextRepetition =
        reviewing.getOneRepetitionForUser(testabilitySettings.getRandomizer()).orElse(null);
    return answerResult;
  }

  @GetMapping(path = "/answers/{answer}")
  @Transactional
  public AnswerViewedByUser getAnswer(@PathVariable("answer") Answer answer) {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(answer.getQuestion().getReviewPoint());
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    AnswerViewedByUser answerResult = answerModel.getAnswerViewedByUser();
    answerResult.reviewPoint = answer.getQuestion().getReviewPoint();
    answerResult.quizQuestion =
        new QuizQuestionViewedByUser(user, answer.getQuestion(), modelFactoryService);
    return answerResult;
  }
}
