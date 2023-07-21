package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.entities.json.AiGeneratedImage;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {
  private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public RestAiController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @PostMapping("/{note}/completion")
  public AiCompletion getCompletion(
      @PathVariable(name = "note") Note note,
      @RequestBody AiCompletionRequest aiCompletionRequest) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getAiCompletion(aiCompletionRequest, note.getPath());
  }

  @PostMapping("/regenerate-question")
  public QuizQuestion regenerateQuestion(
      @RequestParam(value = "note") Note note, @RequestBody String question)
      throws QuizQuestionNotPossibleException {
    return getQuizQuestion(note, question);
  }

  private QuizQuestion getQuizQuestion(Note note, String question)
      throws QuizQuestionNotPossibleException {
    currentUser.assertLoggedIn();
    String rawJsonQuestion = aiAdvisorService.generateQuestionJsonString(note, question);
    QuizQuestionEntity quizQuestionEntity = new QuizQuestionEntity();
    quizQuestionEntity.setQuestionType(QuizQuestionEntity.QuestionType.AI_QUESTION);
    quizQuestionEntity.setRawJsonQuestion(rawJsonQuestion);
    return modelFactoryService.toQuizQuestion(quizQuestionEntity, currentUser.getEntity());
  }

  @PostMapping("/generate-image")
  public AiGeneratedImage generateImage(@RequestBody AiCompletionRequest aiCompletionRequest) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(aiAdvisorService.getImage(aiCompletionRequest.prompt));
  }
}
