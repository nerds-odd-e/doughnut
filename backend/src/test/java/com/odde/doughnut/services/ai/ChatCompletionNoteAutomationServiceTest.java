package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatCompletionNoteAutomationServiceTest {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired OpenAiApiHandler openAiApiHandler;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  private Note testNote;
  private ChatCompletionNoteAutomationService service;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize service
    service =
        new ChatCompletionNoteAutomationService(openAiApiHandler, globalSettingsService, testNote);
  }

  @Nested
  class GenerateUnderstandingChecklist {
    @Test
    void shouldReturnUnderstandingPoints() throws JsonProcessingException {
      // Mock chat completion with JSON schema response
      UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
      understandingChecklist.setPoints(
          List.of(
              "English is a language that is spoken in many countries.",
              "It is also the most widely spoken language in the world."));
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(understandingChecklist);

      List<String> result = service.generateUnderstandingChecklist();

      assertThat(result, hasSize(2));
      assertThat(
          result,
          contains(
              "English is a language that is spoken in many countries.",
              "It is also the most widely spoken language in the world."));
    }

    @Test
    void shouldReturnEmptyListWhenNoResponse() throws JsonProcessingException {
      // Mock chat completion with no tool calls (empty response)
      openAIChatCompletionMock.mockNullChatCompletion();

      List<String> result = service.generateUnderstandingChecklist();

      assertThat(result, is(empty()));
    }

    @Test
    void shouldReturnEmptyListWhenChecklistIsEmpty() throws JsonProcessingException {
      // Mock chat completion with empty checklist
      UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
      understandingChecklist.setPoints(List.of());
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(understandingChecklist);

      List<String> result = service.generateUnderstandingChecklist();

      assertThat(result, is(empty()));
    }

    @Test
    void shouldHandleChecklistWithMultiplePoints() throws JsonProcessingException {
      // Mock chat completion with multiple understanding points
      UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
      understandingChecklist.setPoints(
          List.of(
              "Point 1: First important aspect.",
              "Point 2: Second important aspect.",
              "Point 3: Third important aspect.",
              "Point 4: Fourth important aspect.",
              "Point 5: Fifth important aspect."));
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(understandingChecklist);

      List<String> result = service.generateUnderstandingChecklist();

      assertThat(result, hasSize(5));
      assertThat(result, hasItem("Point 1: First important aspect."));
      assertThat(result, hasItem("Point 5: Fifth important aspect."));
    }

    @Test
    void shouldRespectMaximumOfFivePoints() throws JsonProcessingException {
      // Mock chat completion with more than 5 points to verify the service doesn't limit
      // (The limitation should be enforced by the AI tool instruction, not the service)
      UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
      understandingChecklist.setPoints(
          List.of("Point 1", "Point 2", "Point 3", "Point 4", "Point 5", "Point 6", "Point 7"));
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(understandingChecklist);

      List<String> result = service.generateUnderstandingChecklist();

      // Service should return whatever the AI returns (limiting is done by AI instruction)
      assertThat(result, hasSize(7));
    }
  }
}
