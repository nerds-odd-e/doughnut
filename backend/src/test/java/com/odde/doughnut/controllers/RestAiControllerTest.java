package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionResult;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerTest {
  RestAiController controller;
  UserModel currentUser;
  @Mock OpenAiService openAiService;
  @Autowired MakeMe makeMe;

  AiSuggestionRequest params =
      new AiSuggestionRequest() {
        {
          this.prompt = "Earth";
        }
      };

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestAiController(openAiService, currentUser);
  }

  @Nested
  class AskSuggestion {
    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> new RestAiController(openAiService, makeMe.aNullUserModel()).askSuggestion(params));
    }

    @Test
    void askSuggestionWithRightPrompt() {
      when(openAiService.createCompletion(
              argThat(
                  request -> {
                    assertEquals("Earth", request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildCompletionResult("blue planet"));
      controller.askSuggestion(params);
    }

    @Test
    void askSuggestionAndUseResponse() {
      when(openAiService.createCompletion(any())).thenReturn(buildCompletionResult("blue planet"));
      AiSuggestion aiSuggestion = controller.askSuggestion(params);
      assertEquals("blue planet", aiSuggestion.suggestion());
    }
  }

  @Nested
  class AskEngagingStory {
    Note aNote;

    @BeforeEach
    void setup() {
      aNote = makeMe.aNote("sanskrit").creatorAndOwner(currentUser).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      Note otherPeopleNote = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.askEngagingStories(List.of(otherPeopleNote)));
    }

    @Test
    void askEngagingStoryWithRightPrompt() throws UnexpectedNoAccessRightException {
      when(openAiService.createCompletion(
              argThat(
                  request -> {
                    assertEquals(
                        "Tell me an engaging story to learn about sanskrit.", request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildCompletionResult("This is an engaging story."));
      controller.askEngagingStories(Collections.singletonList(aNote));
    }

    @Test
    void askEngagingStoryWithRightMaxTokens() throws UnexpectedNoAccessRightException {
      // We are testing on maxTokens, because changing this setting will have an effect on request
      // performance.
      // When this setting is changed please do a manual test that engaging stories can still be
      // requested.
      when(openAiService.createCompletion(
              argThat(
                  request -> {
                    assertEquals(150, request.getMaxTokens());
                    return true;
                  })))
          .thenReturn(buildCompletionResult("This is an engaging story."));
      controller.askEngagingStories(Collections.singletonList(aNote));
    }

    @Test
    void askEngagingStoryReturnsEngagingStory() throws UnexpectedNoAccessRightException {
      when(openAiService.createCompletion(Mockito.any()))
          .thenReturn(buildCompletionResult("This is an engaging story."));
      final AiEngagingStory aiEngagingStory =
          controller.askEngagingStories(Collections.singletonList(aNote));
      assertEquals("This is an engaging story.", aiEngagingStory.engagingStory());
    }

    @Test
    void askEngagingStoryForMultipleNotes_returnsEngagingStory()
        throws UnexpectedNoAccessRightException {
      when(openAiService.createCompletion(
              argThat(
                  request -> {
                    assertEquals(
                        "Tell me an engaging story to learn about sanskrit and mandala.",
                        request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildCompletionResult("This is an engaging story."));
      Note anotherNote = makeMe.aNote("mandala").creatorAndOwner(currentUser).please();
      controller.askEngagingStories(List.of(aNote, anotherNote));
    }
  }

  @NotNull
  private CompletionResult buildCompletionResult(String text) {
    return makeMe.openAiCompletionResult().choice(text).please();
  }
}
