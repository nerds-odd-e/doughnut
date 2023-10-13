package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.ApiError;
import com.odde.doughnut.controllers.json.QuestionSuggestionCreationParams;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.FeedbackExistingException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz-questions")
class RestQuizQuestionController {
  private final ModelFactoryService modelFactoryService;

  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestQuizQuestionController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/{quizQuestion}/answer")
  @Transactional
  public AnsweredQuestion answerQuiz(
      @PathVariable("quizQuestion") QuizQuestionEntity quizQuestionEntity,
      @Valid @RequestBody Answer answer) {
    currentUser.assertLoggedIn();
    answer.setQuestion(quizQuestionEntity);
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    answerModel.makeAnswerToQuestion(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return answerModel.getAnswerViewedByUser(currentUser.getEntity());
  }

  @PostMapping("/{quizQuestion}/suggest-fine-tuning")
  @Transactional
  public ResponseEntity<?> suggestQuestionForFineTuning(
      @PathVariable("quizQuestion") QuizQuestionEntity quizQuestionEntity,
      @Valid @RequestBody QuestionSuggestionCreationParams suggestion) {
    try {
      SuggestedQuestionForFineTuning sqft = new SuggestedQuestionForFineTuning();
      var suggestedQuestionForFineTuningService =
          modelFactoryService.toSuggestedQuestionForFineTuningService(sqft);
      return new ResponseEntity<>(
          suggestedQuestionForFineTuningService.suggestQuestionForFineTuning(
              quizQuestionEntity,
              suggestion,
              currentUser.getEntity(),
              testabilitySettings.getCurrentUTCTimestamp()),
          HttpStatus.OK);
    } catch (FeedbackExistingException e) {
      var apiError =
          new ApiError(
              "You have already submitted a feedback", ApiError.ErrorType.EXISTING_FEEDBACK_ERROR);
      apiError.add("errorType", ApiError.ErrorType.EXISTING_FEEDBACK_ERROR.toString());
      return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
  }
}
