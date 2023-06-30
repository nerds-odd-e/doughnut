package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.AiCompetionRequest;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteMessageBuilder;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
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
      @PathVariable(name = "note") Note note, @RequestBody AiCompetionRequest aiCompetionRequest) {
    currentUser.assertLoggedIn();
    List<ChatMessage> messages =
        new OpenAIChatAboutNoteMessageBuilder(note)
            .instructionForCompletion(aiCompetionRequest)
            .build();
    return aiAdvisorService.getCompletion(messages, aiCompetionRequest.incompleteAssistantMessage);
  }

  @PostMapping("/generate-question")
  public QuizQuestion generateQuestion(@RequestParam(value = "note") Note note)
      throws QuizQuestionNotPossibleException {
    currentUser.assertLoggedIn();
    String rawJsonQuestion = aiAdvisorService.generateQuestionJsonString(note);
    QuizQuestionEntity quizQuestionEntity = new QuizQuestionEntity();
    quizQuestionEntity.setQuestionType(QuizQuestionEntity.QuestionType.AI_QUESTION);
    quizQuestionEntity.setRawJsonQuestion(rawJsonQuestion);
    return modelFactoryService.toQuizQuestion(quizQuestionEntity, currentUser.getEntity());
  }

  @PostMapping("/ask-engaging-stories")
  public AiEngagingStory askEngagingStories(@RequestBody AiCompetionRequest aiCompetionRequest) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getEngagingStory(aiCompetionRequest.prompt);
  }
}
