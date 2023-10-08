package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

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
      @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getAiCompletion(aiCompletionParams, note.getContextPathString(), note);
  }

  @PostMapping("/chat")
  public ChatResponse chat(
      @RequestParam(value = "note") Note note, @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    String userMessage = request.getUserMessage();
    String assistantMessage = this.aiAdvisorService.chatToAi(note, userMessage);
    return new ChatResponse(assistantMessage);
  }

  @PostMapping("/generate-question")
  public QuizQuestion generateQuestion(@RequestParam(value = "note") Note note) {
    currentUser.assertLoggedIn();
    QuizQuestionServant servant =
        new QuizQuestionServant(
            currentUser.getEntity(), null, modelFactoryService, aiAdvisorService);
    try {
      QuizQuestionEntity quizQuestionEntity =
          new QuizQuestionDirector(QuizQuestionEntity.QuestionType.AI_QUESTION, servant)
              .invoke(note.getThing());
      modelFactoryService.quizQuestionRepository.save(quizQuestionEntity);
      return modelFactoryService.toQuizQuestion(quizQuestionEntity, currentUser.getEntity());
    } catch (QuizQuestionNotPossibleException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated", e);
    }
  }

  @PostMapping("/generate-image")
  public AiGeneratedImage generateImage(@RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(aiAdvisorService.getImage(aiCompletionParams.prompt));
  }
}
