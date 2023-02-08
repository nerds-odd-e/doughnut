package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.TextContent;
import com.odde.doughnut.entities.json.AiStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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
    MockitoAnnotations.openMocks(this);
    controller = new RestAiController(openAiService);
  }

  @Test
  void askSuggestionWithRightParams() {
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals("Tell me about Earth.", request.getPrompt());
                  assertEquals(3000, request.getMaxTokens());
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
  void streamSuggestions() {
    when(openAiService.createCompletion(any())).thenReturn(buildCompletionResult("Harry Potter"));
    Flux<AiSuggestion> aiSuggestion = controller.streamSuggestions();
    StepVerifier.create(aiSuggestion)
        .expectNext(new AiSuggestion("Harry Potter"))
        .expectComplete()
        .verify();
  }

  @Test
  void askEngagingStoryWithRightParams() {
    CompletionRequest completionRequest =
        CompletionRequest.builder()
            .prompt("Tell me an engaging story to learn about Coming soon")
            .model("text-davinci-003")
            .maxTokens(3000)
            .echo(true)
            .build();

    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals(
                      "Tell me an engaging story to learn about Coming soon", request.getPrompt());
                  assertEquals(3000, request.getMaxTokens());
                  return true;
                })))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    final TextContent textContent = new TextContent();
    textContent.setTitle("Coming soon");
    final AiStory aiStory =
        controller.askStory(
            Note.createNote(null, new Timestamp(System.currentTimeMillis()), textContent));
    assertEquals("This is an engaging story.", aiStory.story());
    verify(openAiService, times(1)).createCompletion(completionRequest);
  }

  @Test
  void askEngagingStoryFor1NoteAnd1ChildNote() {
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals(
                      "Tell me an engaging story to learn about Coming soon parentComing soon child",
                      request.getPrompt());
                  return true;
                })))
        .thenReturn(buildCompletionResult("This is an engaging story."));

    Note parentNote = makeMe.aNote("Coming soon parent").please();
    makeMe.aNote("Coming soon child").under(parentNote).please();
    makeMe.refresh(parentNote);
    controller.askStory(parentNote);
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
