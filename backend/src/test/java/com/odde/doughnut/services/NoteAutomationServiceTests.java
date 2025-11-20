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
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteAutomationServiceTests {
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingsService globalSettingsService;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  private Note testNote;
  private NoteAutomationService service;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize common services
    OpenAiApiHandler openAiApiHandler = new OpenAiApiHandler(openAiApi);
    ObjectMapper objectMapper = getTestObjectMapper();
    ChatCompletionNoteAutomationService chatCompletionNoteAutomationService =
        new ChatCompletionNoteAutomationService(
            openAiApiHandler, globalSettingsService, objectMapper, testNote);
    service = new NoteAutomationService(chatCompletionNoteAutomationService);
  }

  @Test
  void shouldHandleNoToolCallWhenSuggestingTitle() throws JsonProcessingException {
    // Mock chat completion with no tool calls (empty response with tools)
    // Note: mockNullChatCompletion only works for requests without tools
    // For requests with tools, we need to return an empty result
    ChatCompletionResult emptyResult = new ChatCompletionResult();
    emptyResult.setChoices(new ArrayList<>());
    Mockito.doReturn(Single.just(emptyResult))
        .when(openAiApi)
        .createChatCompletion(
            ArgumentMatchers.argThat(
                request -> request.getTools() != null && !request.getTools().isEmpty()));

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
