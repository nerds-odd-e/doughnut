
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
class RestReviewsController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;


  public RestReviewsController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher, TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  public Reviewing overview() {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    return user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
  }

  @GetMapping("/initial")
  public ReviewPointViewedByUser initialReview() {
    UserModel user = currentUserFetcher.getUser();
    Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    ReviewPoint reviewPoint = reviewing.getOneInitialReviewPoint();
    return ReviewPointViewedByUser.from(reviewPoint, user);
  }

  static class InitialInfo {
      @Valid
      public ReviewPoint reviewPoint;
      @Valid
      public ReviewSetting reviewSetting;
  }
  @PostMapping(path="")
  @Transactional
  public ReviewPointViewedByUser create(@RequestBody InitialInfo initialInfo) {
    UserModel userModel = currentUserFetcher.getUser();
    if(initialInfo.reviewPoint.getNoteId() != null) {
      initialInfo.reviewPoint.setNote(modelFactoryService.noteRepository.findById(initialInfo.reviewPoint.getNoteId()).orElse(null));
    }
    if(initialInfo.reviewPoint.getLinkId() != null) {
      initialInfo.reviewPoint.setLink(modelFactoryService.linkRepository.findById(initialInfo.reviewPoint.getLinkId()).orElse(null));
    }
    ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(initialInfo.reviewPoint);
    reviewPointModel.initialReview(userModel, initialInfo.reviewSetting, testabilitySettings.getCurrentUTCTimestamp());
    return initialReview();
  }

  static class RepetitionForUser {
    @Getter @Setter
    private ReviewPointViewedByUser reviewPointViewedByUser;
    @Getter @Setter
    private QuizQuestion quizQuestion;
    @Getter @Setter
    private Answer emptyAnswer;
    @Getter @Setter
    private Integer toRepeatCount;
  }

  @GetMapping("/repeat")
  public RepetitionForUser repeatReview() {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    ReviewPointModel reviewPointModel = reviewing.getOneReviewPointNeedToRepeat(testabilitySettings.getRandomizer());

    RepetitionForUser repetitionForUser = new RepetitionForUser();

    if(reviewPointModel != null) {
      repetitionForUser.setReviewPointViewedByUser(ReviewPointViewedByUser.from(reviewPointModel.getEntity(), user));
      QuizQuestion quizQuestion = reviewPointModel.generateAQuizQuestion(testabilitySettings.getRandomizer());
      if (quizQuestion != null) {
          repetitionForUser.setQuizQuestion(quizQuestion);
          repetitionForUser.setEmptyAnswer(quizQuestion.buildAnswer());
      }
    }
    repetitionForUser.setToRepeatCount(reviewing.toRepeatCount());
    return repetitionForUser;
  }

  @PostMapping("/{reviewPoint}/answer")
  public AnswerResult answerQuiz(ReviewPoint reviewPoint, @Valid @RequestBody Answer answer) {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
    AnswerResult answerResult = new AnswerResult();
    answerResult.setReviewPoint(reviewPoint);
    answerResult.setQuestionType(answer.getQuestionType());
    answerResult.setAnswer(answer.getAnswer());
    if (answer.getAnswerNoteId() != null) {
      answerResult.setAnswerNote(modelFactoryService.noteRepository.findById(answer.getAnswerNoteId()).orElse(null));
    }
    return answerResult;
  }

  @PostMapping(path="/{reviewPoint}/self-evaluate")
  public RepetitionForUser selfEvaluate(ReviewPoint reviewPoint, @RequestBody String selfEvaluation) {
    UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertLoggedIn();
     evaluate(reviewPoint, selfEvaluation);
     return repeatReview();
  }

  private int evaluate(ReviewPoint reviewPoint, String selfEvaluation) {
    if (selfEvaluation.equals("\"again\"")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).increaseRepetitionCountAndSave();
    }
    if (selfEvaluation.equals("\"satisfying\"")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).repeated(testabilitySettings.getCurrentUTCTimestamp());
    }
    if (selfEvaluation.equals("\"sad\"")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).repeatedSad(testabilitySettings.getCurrentUTCTimestamp());
    }
    if (selfEvaluation.equals("\"happy\"")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).repeatedHappy(testabilitySettings.getCurrentUTCTimestamp());
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }

}
