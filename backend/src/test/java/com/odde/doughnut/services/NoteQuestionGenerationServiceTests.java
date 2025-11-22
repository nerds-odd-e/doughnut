package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteQuestionGenerationServiceTests {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired MakeMe makeMe;
  @Autowired NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  private Note testNote;
  private NoteQuestionGenerationService service;

  @BeforeEach
  void setup() {
    // Initialize OpenAIChatCompletionMock
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();

    // Initialize common services
    service = notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(testNote);
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
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());

      boolean hasQuestionDesignerInstruction =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .anyMatch(msg -> msg.contains("Please act as a Question Designer"));

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
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());

      assertThat(paramsCaptor.getValue().model().asString(), is("gpt-4o-mini"));
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
  }

  @Nested
  class BuildQuestionGenerationRequest {
    @Test
    void shouldBuildRequestWithNoteDescription() {
      ChatCompletionRequest request = service.buildQuestionGenerationRequest(null);

      assertThat(request, is(notNullValue()));
      assertThat(request.getModel(), is("gpt-4o-mini"));
      boolean hasNoteDescription =
          request.getMessages().stream()
              .filter(message -> message instanceof SystemMessage)
              .anyMatch(
                  message ->
                      message.toString().contains("Focus Note and the notes related to it:"));
      assertThat("Request should contain note description", hasNoteDescription, is(true));
    }

    @Test
    void shouldBuildRequestWithQuestionGenerationInstruction() {
      ChatCompletionRequest request = service.buildQuestionGenerationRequest(null);

      boolean hasQuestionDesignerInstruction =
          request.getMessages().stream()
              .filter(message -> message instanceof UserMessage)
              .anyMatch(
                  message -> message.toString().contains("Please act as a Question Designer"));

      assertThat(
          "Request should contain Question Designer instruction",
          hasQuestionDesignerInstruction,
          is(true));
    }

    @Test
    void shouldIncludeNotebookAssistantInstructionsWhenPresent() {
      NotebookAiAssistant notebookAiAssistant = new NotebookAiAssistant();
      notebookAiAssistant.setNotebook(testNote.getNotebook());
      notebookAiAssistant.setAdditionalInstructionsToAi("Custom notebook instructions");
      Timestamp currentTime = new Timestamp(System.currentTimeMillis());
      notebookAiAssistant.setCreatedAt(currentTime);
      notebookAiAssistant.setUpdatedAt(currentTime);
      makeMe.entityPersister.save(notebookAiAssistant);
      makeMe.refresh(testNote.getNotebook());
      service =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(testNote);

      ChatCompletionRequest request = service.buildQuestionGenerationRequest(null);

      boolean hasNotebookInstructions =
          request.getMessages().stream()
              .filter(message -> message instanceof SystemMessage)
              .anyMatch(message -> message.toString().contains("Custom notebook instructions"));

      assertThat(
          "Request should contain notebook assistant instructions",
          hasNotebookInstructions,
          is(true));
    }

    @Test
    void shouldIncludeAdditionalMessageWhenProvided() {
      MessageRequest additionalMessage = new MessageRequest();
      additionalMessage.setContent("Generate a question about the capital city");

      ChatCompletionRequest request = service.buildQuestionGenerationRequest(additionalMessage);

      boolean hasAdditionalMessage =
          request.getMessages().stream()
              .filter(message -> message instanceof UserMessage)
              .anyMatch(
                  message ->
                      message.toString().contains("Generate a question about the capital city"));

      assertThat("Request should contain additional message", hasAdditionalMessage, is(true));
    }

    @Test
    void shouldNotIncludeNotebookAssistantInstructionsWhenEmpty() {
      // No NotebookAiAssistant created, so instructions should be null/empty
      ChatCompletionRequest request = service.buildQuestionGenerationRequest(null);

      long systemMessageCount =
          request.getMessages().stream()
              .filter(message -> message instanceof SystemMessage)
              .count();

      assertThat(
          "Request should have only one system message (note description)",
          systemMessageCount,
          is(1L));
    }
  }
}
