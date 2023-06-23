package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.models.UserModel;
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
    aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @PostMapping("/{note}/ask-suggestions")
  public AiSuggestion askSuggestion(
      @PathVariable(name = "note") Note note,
      @RequestBody AiSuggestionRequest aiSuggestionRequest) {
    currentUser.assertLoggedIn();
    NoteModel noteModel = modelFactoryService.toNoteModel(note);
    return aiAdvisorService.getAiSuggestion(noteModel.getPath(), aiSuggestionRequest);
  }

  @GetMapping("/generate-question")
  public AiSuggestion generateQuestion(@RequestParam(value = "note") Note note) {
    currentUser.assertLoggedIn();
    NoteModel noteModel = modelFactoryService.toNoteModel(note);
    return aiAdvisorService.generateQuestion(noteModel.getPath(), questionPrompt(note));
  }

  private String questionPrompt(Note note) {
    return """
      Given the note with title: %s
      and description:
      %s

      please generate a multiple-choice question with 3 options and 1 correct option.
      Please vary the option text length, so that the correct answer isn't always the longest one.
      The response should be JSON-formatted as follows:
        {
          question: "",
          options: [
            {
              option: "",
              correct: true,
              explanation: "",
            },
          ],
        }
      )}"""
        .formatted(note.getTitle(), note.getTextContent().getDescription());
  }

  @PostMapping("/ask-engaging-stories")
  public AiEngagingStory askEngagingStories(@RequestBody AiSuggestionRequest aiSuggestionRequest) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getEngagingStory(aiSuggestionRequest.prompt);
  }
}
