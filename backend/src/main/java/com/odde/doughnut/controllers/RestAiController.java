package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
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

  @PostMapping("/{note}/ask-suggestions")
  public AiSuggestion askSuggestion(
      @PathVariable(name = "note") Note note,
      @RequestBody AiSuggestionRequest aiSuggestionRequest) {
    currentUser.assertLoggedIn();
    NoteModel noteModel = modelFactoryService.toNoteModel(note);
    List<ChatMessage> messages =
        noteModel.getChatMessagesForNoteDescriptionCompletion(aiSuggestionRequest);
    return aiAdvisorService.getAiSuggestion(
        messages, aiSuggestionRequest.incompleteAssistantMessage);
  }

  @GetMapping("/generate-question")
  public QuizQuestionViewedByUser generateQuestion(@RequestParam(value = "note") Note note) {
    currentUser.assertLoggedIn();
    String rawJsonQuestion = aiAdvisorService.generateQuestionJsonString(note, modelFactoryService);
    QuizQuestion quizQuestion = new QuizQuestion();
    quizQuestion.setQuestionType(QuizQuestion.QuestionType.AI_QUESTION);
    quizQuestion.setRawJsonQuestion(rawJsonQuestion);
    return new QuizQuestionViewedByUser(quizQuestion, null, null);
  }

  @PostMapping("/ask-engaging-stories")
  public AiEngagingStory askEngagingStories(@RequestBody AiSuggestionRequest aiSuggestionRequest) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getEngagingStory(aiSuggestionRequest.prompt);
  }
}
