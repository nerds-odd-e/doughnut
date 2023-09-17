package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.DueReviewPoints;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
class RestReviewsController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

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
        .getReviewStatus(currentUser.getTimeZone());
  }

  @GetMapping("/initial")
  @Transactional(readOnly = true)
  public List<ReviewPoint> initialReview() {
    currentUser.assertLoggedIn();
    Reviewing reviewing = currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp());

    return reviewing
        .getDueInitialReviewPoints(currentUser.getTimeZone())
        .collect(Collectors.toList());
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
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    currentUser.assertLoggedIn();
    Reviewing reviewing = currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    ZoneId timeZone = ZoneId.of(timezone);
    return reviewing.getDueReviewPoints(dueInDays, timeZone);
  }

  @GetMapping(path = "/answers/{answer}")
  @Transactional
  public AnsweredQuestion showAnswer(@PathVariable("answer") Answer answer)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(answer);
    return modelFactoryService.toAnswerModel(answer).getAnswerViewedByUser(currentUser.getEntity());
  }
}
