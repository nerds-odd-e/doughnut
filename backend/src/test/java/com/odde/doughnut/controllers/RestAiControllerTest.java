package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerTest {
  RestAiController controller;
  @Mock OpenAiService openAiService;
  @Autowired MakeMe makeMe;

  HashMap<String, String> params =
      new HashMap<>() {
        {
          this.put("title", "Earth");
        }
      };

  @BeforeEach
  void Setup() {
    controller = new RestAiController(openAiService);
  }

  @Test
  void askSuggestionWithRightPrompt() {
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals("Tell me about Earth in a paragraph.", request.getPrompt());
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

  @Test
  void askEngagingStoryWithRightPrompt() {
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals(
                      "Tell me an engaging story to learn about Coming soon.", request.getPrompt());
                  return true;
                })))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    Note aNote = makeMe.aNote("Coming soon").please();
    controller.askEngagingStories(Collections.singletonList(aNote));
  }

  @Test
  void askEngagingStoryWithRightMaxTokens() {
    // We are testing on maxTokens, because changing this setting will have an effect on request
    // performance.
    // When this setting is changed please do a manual test that engaging stories can still be
    // requested.
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals(500, request.getMaxTokens());
                  return true;
                })))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    Note aNote = makeMe.aNote("Coming soon").please();
    controller.askEngagingStories(Collections.singletonList(aNote));
  }

  @Test
  void askEngagingStoryReturnsEngagingStory() {
    when(openAiService.createCompletion(Mockito.any()))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    Note aNote = makeMe.aNote().please();
    final AiEngagingStory aiEngagingStory =
        controller.askEngagingStories(Collections.singletonList(aNote));
    assertEquals("This is an engaging story.", aiEngagingStory.engagingStory());
  }

  @Test
  void askEngagingStoryFor1NoteAnd1ChildNoteReturnsEngagingStory() {
    when(openAiService.createCompletion(any()))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    Note parentNote = makeMe.aNote("Coming soon parent").please();
    makeMe.aNote("Coming soon child").under(parentNote).please();
    makeMe.refresh(parentNote);
    final AiEngagingStory aiEngagingStory =
        controller.askEngagingStories(Collections.singletonList(parentNote));
    assertEquals("This is an engaging story.", aiEngagingStory.engagingStory());
  }

  @Test
  void askEngagingStoryForMultipleNotes_returnsEngagingStory() {
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals(
                      "Tell me an engaging story to learn about note and note2.",
                      request.getPrompt());
                  return true;
                })))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    Note note = makeMe.aNote("note").please();
    Note note2 = makeMe.aNote("note2").please();

    final AiEngagingStory aiEngagingStory = controller.askEngagingStories(List.of(note, note2));
  }

  @NotNull
  private static CompletionResult buildCompletionResult(String text) {
    CompletionResult completionResult = new CompletionResult();
    completionResult.setChoices(
        List.of(
            new CompletionChoice() {
              {
                this.setText(text);
              }
            }));
    return completionResult;
  }
}
