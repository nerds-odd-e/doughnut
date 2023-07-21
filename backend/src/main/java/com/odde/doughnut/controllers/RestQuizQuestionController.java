package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz-question")
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
  public AnswerResult answerQuiz(
      @PathVariable("quizQuestion") QuizQuestionEntity quizQuestionEntity,
      @Valid @RequestBody Answer answer) {
    currentUser.assertLoggedIn();
    answer.setQuestion(quizQuestionEntity);
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    answerModel.updateReviewPoints(testabilitySettings.getCurrentUTCTimestamp());
    answerModel.save();
    return answerModel.getAnswerResult();
  }
}
