
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

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
    return user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
  }

  class RepetitionForUser {
    @Getter @Setter
    private ReviewPointViewedByUser reviewPointViewedByUser;
    @Getter @Setter
    private QuizQuestion quizQuestion;
    @Getter @Setter
    private Answer emptyAnswer;
  }

  @GetMapping("/repeat")
  public RepetitionForUser repeatReview() {
    UserModel user = currentUserFetcher.getUser();
    Reviewing reviewing = user.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    ReviewPointModel reviewPointModel = reviewing.getOneReviewPointNeedToRepeat(testabilitySettings.getRandomizer());

    RepetitionForUser repetitionForUser = new RepetitionForUser();

    if(reviewPointModel != null) {
      repetitionForUser.setReviewPointViewedByUser(ReviewPointViewedByUser.getReviewPointViewedByUser(reviewPointModel.getEntity(), user.getEntity()));
      QuizQuestion quizQuestion = reviewPointModel.generateAQuizQuestion(testabilitySettings.getRandomizer());
      if (quizQuestion != null) {
          repetitionForUser.setQuizQuestion(quizQuestion);
          repetitionForUser.setEmptyAnswer(quizQuestion.buildAnswer());
      }
    }
    return repetitionForUser;
  }

  @PostMapping("/{reviewPoint}/answer")
  public AnswerResult answerQuiz(ReviewPoint reviewPoint, @Valid @RequestBody Answer answer) {
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
  public Integer selfEvaluate(ReviewPoint reviewPoint, @RequestBody String selfEvaluation) {
    if (selfEvaluation.equals("again")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).increaseRepetitionCountAndSave();
    }
    if (selfEvaluation.equals("satisfying")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).repeated(testabilitySettings.getCurrentUTCTimestamp());
    }
    if (selfEvaluation.equals("sad")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).repeatedSad(testabilitySettings.getCurrentUTCTimestamp());
    }
    if (selfEvaluation.equals("happy")) {
        return modelFactoryService.toReviewPointModel(reviewPoint).repeatedHappy(testabilitySettings.getCurrentUTCTimestamp());
    }
    return -1;
  }

}
