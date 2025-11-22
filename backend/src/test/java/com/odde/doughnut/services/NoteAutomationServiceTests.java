package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.ChatCompletionNoteAutomationService;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteAutomationServiceTests {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired OpenAiApiHandler openAiApiHandler;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  private Note testNote;
  private NoteAutomationService service;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize common services
    ObjectMapper objectMapper = getTestObjectMapper();
    ChatCompletionNoteAutomationService chatCompletionNoteAutomationService =
        new ChatCompletionNoteAutomationService(
            openAiApiHandler, globalSettingsService, objectMapper, testNote);
    service = new NoteAutomationService(chatCompletionNoteAutomationService);
  }

  @Test
  void shouldHandleNoToolCallWhenSuggestingTitle() throws JsonProcessingException {
    // Mock chat completion with no tool calls (empty response with tools)
    // Use OpenAIChatCompletionMock to return an empty response
    openAIChatCompletionMock.mockNullChatCompletion();

    String result = service.suggestTitle();

    assertThat(result, is(nullValue()));
  }

  @Test
  void shouldReturnSuggestedTitle() throws JsonProcessingException {
    // Mock chat completion with tool call
    TitleReplacement titleReplacement = new TitleReplacement();
    titleReplacement.setNewTitle("Suggested Title");
    openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(
        titleReplacement, AiToolName.SUGGEST_NOTE_TITLE.getValue());

    String result = service.suggestTitle();

    assertThat(result, is("Suggested Title"));
  }

  private ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }
}
