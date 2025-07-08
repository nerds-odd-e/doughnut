package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteQuestionGenerationServiceTests {
  @Mock OpenAiApi openAiApi;
  GlobalSettingsService globalSettingsService;
  @Autowired MakeMe makeMe;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  private Note testNote;
  private OpenAiAssistant assistant;
  private NoteQuestionGenerationService service;

  @BeforeEach
  void setup() {
    // Initialize OpenAIChatCompletionMock
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize common services
    OpenAiApiHandler openAiApiHandler = new OpenAiApiHandler(openAiApi);
    globalSettingsService = new GlobalSettingsService(makeMe.modelFactoryService);
    service = new NoteQuestionGenerationService(globalSettingsService, testNote, openAiApiHandler);
  }

  @Nested
  class GenerateQuestion {
    @Test
    void shouldGenerateQuestionWithCorrectStem() throws Exception {
      // Mock AI response
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      // Execute
      MCQWithAnswer generatedQuestion = service.generateQuestion(null);

      // Verify
      assertThat(
          generatedQuestion.getMultipleChoicesQuestion().getStem(),
          containsString("What is the first color in the rainbow?"));
    }

    @Test
    void shouldPassQuestionGenerationInstructionAsUserMessage() throws JsonProcessingException {
      // Mock AI response
      MCQWithAnswer mcqWithAnswer =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the capital of France?")
              .choices("Paris", "London", "Berlin")
              .correctChoiceIndex(0)
              .please();

      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      // Execute
      service.generateQuestion(null);

      // Verify
      ArgumentCaptor<ChatCompletionRequest> requestCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletion(requestCaptor.capture());

      // Check if any message contains the expected text
      boolean hasQuestionDesignerInstruction =
          requestCaptor.getValue().getMessages().stream()
              .anyMatch(
                  message -> message.toString().contains("Please act as a Question Designer"));

      assertThat(
          "A message should contain the Question Designer instruction",
          hasQuestionDesignerInstruction,
          is(true));
    }

    @Test
    void shouldUseModelNameFromGlobalSettings() throws JsonProcessingException {
      MCQWithAnswer mcqWithAnswer =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the capital of France?")
              .choices("Paris", "London", "Berlin")
              .correctChoiceIndex(0)
              .please();

      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      // Act
      service.generateQuestion(null);

      // Verify
      ArgumentCaptor<ChatCompletionRequest> requestCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletion(requestCaptor.capture());

      assertThat(requestCaptor.getValue().getModel(), is("gpt-4o-mini"));
    }

    @Test
    void shouldHandleNullChatCompletion() throws JsonProcessingException {
      // Mock AI response for null completion
      openAIChatCompletionMock.mockNullChatCompletion();

      // Execute and verify
      MCQWithAnswer result = service.generateQuestion(null);

      // Verify that a null completion returns null
      assertThat(result, is(nullValue()));
    }

    @Test
    void shouldOutputCreatedAtInISOFormatInSystemMessage() throws Exception {
      // Arrange: set up a note with a known createdAt
      Note note = makeMe.aNote().please();
      OpenAiApiHandler openAiApiHandler = new OpenAiApiHandler(openAiApi);
      NoteQuestionGenerationService serviceWithNote =
          new NoteQuestionGenerationService(globalSettingsService, note, openAiApiHandler);
      MCQWithAnswer jsonQuestion = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      // Act
      serviceWithNote.generateQuestion(null);

      // Capture the system message (note description)
      ArgumentCaptor<ChatCompletionRequest> requestCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletion(requestCaptor.capture());
      String systemMessage = requestCaptor.getValue().getMessages().get(0).getTextContent();

      // Extract createdAt from the JSON in the system message
      java.util.regex.Matcher matcher =
          java.util.regex.Pattern.compile("\\\"createdAt\\\"\\s*:\\s*([^,\n\r}]*)")
              .matcher(systemMessage);
      assertThat(
          "createdAt field should be present in the system message", matcher.find(), is(true));
      String createdAtValue = matcher.group(1).trim();
      // It should start with a quote if it's ISO string, or be a number if not
      assertThat(
          "createdAt should be in ISO string format, but was: " + createdAtValue,
          createdAtValue.startsWith("\""),
          is(true));
    }
  }
}
