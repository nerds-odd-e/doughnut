package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.AnswerModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.PredefinedQuestionService;
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
  private final ModelFactoryService modelFactoryService;
  private final PredefinedQuestionService predefinedQuestionService;

  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewQuestionController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.predefinedQuestionService = new PredefinedQuestionService(openAiApi, modelFactoryService);
  }

  @PostMapping("/generate-question")
  @Transactional
  public ReviewQuestionInstance generateQuestion(
      @RequestParam(value = "note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    PredefinedQuestion question = predefinedQuestionService.generateQuestionForNote(note);
    if (question == null) {
      return null;
    }
    return modelFactoryService.createReviewQuestion(question);
  }

  @PostMapping("/{reviewQuestionInstance}/regenerate")
  @Transactional
  public ReviewQuestionInstance regenerate(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance) {
    currentUser.assertLoggedIn();
    PredefinedQuestion question =
        predefinedQuestionService.generateQuestionForNote(
            reviewQuestionInstance.getPredefinedQuestion().getNote());
    if (question == null) {
      return null;
    }
    return modelFactoryService.createReviewQuestion(question);
  }

  @PostMapping("/{reviewQuestionInstance}/contest")
  @Transactional
  public ReviewQuestionContestResult contest(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance) {
    currentUser.assertLoggedIn();
    return predefinedQuestionService.contest(reviewQuestionInstance.getPredefinedQuestion());
  }

  @PostMapping("/{reviewQuestionInstance}/answer")
  @Transactional
  public AnsweredQuestion answerQuiz(
      @PathVariable("reviewQuestionInstance") @Schema(type = "integer")
          ReviewQuestionInstance reviewQuestionInstance,
      @Valid @RequestBody AnswerDTO answerDTO) {
    currentUser.assertLoggedIn();
    Answer answer = new Answer();
    answer.setReviewQuestionInstance(reviewQuestionInstance);
    answer.setFromDTO(answerDTO);
    AnswerModel answerModel = modelFactoryService.toAnswerModel(answer);
    answerModel.makeAnswerToQuestion(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return answerModel.getAnswerViewedByUser(currentUser.getEntity());
  }
}
