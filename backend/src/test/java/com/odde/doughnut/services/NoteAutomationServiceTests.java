package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.ChatCompletionNoteAutomationService;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;
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
    ChatCompletionNoteAutomationService chatCompletionNoteAutomationService =
        new ChatCompletionNoteAutomationService(openAiApiHandler, globalSettingsService, testNote);
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
    // Mock chat completion with JSON schema response
    TitleReplacement titleReplacement = new TitleReplacement();
    titleReplacement.setNewTitle("Suggested Title");
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(titleReplacement);

    String result = service.suggestTitle();

    assertThat(result, is("Suggested Title"));
  }

  @Test
  void shouldThrowExceptionWhenRequestHasTools() {
    // Create a minimal request with tools (which should trigger the guard)
    // Note: We don't use responseFormat here to avoid type issues - we just need tools present
    ChatCompletionCreateParams requestWithTools =
        ChatCompletionCreateParams.builder()
            .model(com.openai.models.ChatModel.of(GlobalSettingsService.DEFAULT_CHAT_MODEL))
            .messages(
                List.of(
                    com.openai.models.chat.completions.ChatCompletionMessageParam.ofUser(
                        com.openai.models.chat.completions.ChatCompletionUserMessageParam.builder()
                            .content("test")
                            .build())))
            .addTool(com.odde.doughnut.services.ai.NoteDetailsCompletion.class)
            .build();

    // Create a custom builder that returns the request with tools
    OpenAIChatRequestBuilder builderWithTools =
        new OpenAIChatRequestBuilder() {
          @Override
          public com.openai.models.chat.completions.ChatCompletionCreateParams build() {
            return requestWithTools;
          }
        };

    // Verify that requestAndGetJsonSchemaResult throws RuntimeException when tools are present
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                openAiApiHandler.requestAndGetJsonSchemaResult(
                    AiToolFactory.suggestNoteTitleAiTool(), builderWithTools));

    assertThat(
        exception.getMessage(),
        containsString("requestAndGetJsonSchemaResult must not be used with tools"));
  }
}
