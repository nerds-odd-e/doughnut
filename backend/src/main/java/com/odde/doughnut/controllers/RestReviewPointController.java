package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.util.List;
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

  public RestReviewPointController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/{reviewPoint}")
  public ReviewPoint show(
      @PathVariable("reviewPoint") @Schema(type = "integer") ReviewPoint reviewPoint)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(reviewPoint);
    return reviewPoint;
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

  @GetMapping("/recent")
  public List<ReviewPoint> getRecentReviewPoints() {
    currentUser.assertLoggedIn();
    return modelFactoryService.reviewPointRepository.findLast100ByUser(
        currentUser.getEntity().getId());
  }

  @GetMapping("/recently-reviewed")
  public List<ReviewPoint> getRecentlyReviewedPoints() {
    currentUser.assertLoggedIn();
    return modelFactoryService.reviewPointRepository.findLast100ReviewedByUser(
        currentUser.getEntity().getId());
  }
}
