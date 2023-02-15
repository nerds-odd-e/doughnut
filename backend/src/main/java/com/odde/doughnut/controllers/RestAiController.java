package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {
  AiAdvisorService aiAdvisorService;
  private UserModel currentUser;

  public RestAiController(
      @Qualifier("testableOpenAiService") OpenAiService openAiService,
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      UserModel currentUser) {
    aiAdvisorService = new AiAdvisorService(openAiService, openAiApi);
    this.currentUser = currentUser;
  }

  @PostMapping("/ask-suggestions")
  public AiSuggestion askSuggestion(@RequestBody AiSuggestionRequest aiSuggestionRequest) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getAiSuggestion(aiSuggestionRequest.prompt);
  }

  @GetMapping("/ask-engaging-stories")
  public AiEngagingStory askEngagingStories(@RequestParam List<Note> notes)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(notes);
    List<String> titles = notes.stream().map(Note::getTitle).toList();
    return aiAdvisorService.getEngagingStory(titles);
  }
}
