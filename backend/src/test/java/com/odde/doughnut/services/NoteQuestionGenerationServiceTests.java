package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.sql.Timestamp;
import java.util.Optional;
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

  @Autowired MakeMe makeMe;
  @Autowired NoteQuestionGenerationService service;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  private Note testNote;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    testNote = makeMe.aNote().please();
    makeMe.aNote().please();
  }

  private boolean systemMessageContains(ChatCompletionCreateParams request, String text) {
    return request.messages().stream()
        .filter(message -> message.system().isPresent())
        .anyMatch(message -> message.system().get().content().toString().contains(text));
  }

  private boolean userMessageContains(ChatCompletionCreateParams request, String text) {
    return request.messages().stream()
        .filter(message -> message.user().isPresent())
        .anyMatch(message -> message.toString().contains(text));
  }

  @Nested
  class GenerateQuestion {
    @Test
    void shouldGenerateQuestionWithCorrectStem() throws Exception {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      MCQWithAnswer generatedQuestion = service.generateQuestion(testNote, null);

      assertThat(
          generatedQuestion.getF0__multipleChoicesQuestion().getF0__stem(),
          containsString("What is the first color in the rainbow?"));
    }

    @Test
    void shouldPassQuestionGenerationInstructionAsUserMessage() throws JsonProcessingException {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      service.generateQuestion(testNote, null);

      ArgumentCaptor<ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      assertThat(
          systemMessageContains(paramsCaptor.getValue(), "Please act as a Question Designer"),
          is(true));
    }

    @Test
    void shouldUseModelNameFromGlobalSettings() throws JsonProcessingException {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      service.generateQuestion(testNote, null);

      ArgumentCaptor<ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      assertThat(
          paramsCaptor.getValue().model().asString(), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
    }

    @Test
    void shouldHandleNullChatCompletion() throws JsonProcessingException {
      openAIChatCompletionMock.mockNullChatCompletion();

      MCQWithAnswer result = service.generateQuestion(testNote, null);

      assertThat(result, is(nullValue()));
    }
  }

  @Nested
  class BuildQuestionGenerationRequest {

    @Test
    void shouldBuildRequestWithNoteDescription() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      assertThat(request, is(notNullValue()));
      assertThat(request.model().toString(), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
      assertThat(userMessageContains(request, "Focus Note and the notes related to it:"), is(true));
    }

    @Test
    void shouldBuildRequestWithNoteInstructions() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      assertThat(userMessageContains(request, "The JSON below is visible only to you"), is(true));
    }

    @Test
    void shouldBuildRequestWithQuestionGenerationInstruction() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      assertThat(systemMessageContains(request, "Please act as a Question Designer"), is(true));
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

      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      assertThat(systemMessageContains(request, "Custom notebook instructions"), is(true));
    }

    @Test
    void shouldIncludeAdditionalMessageWhenProvided() {
      ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(
              testNote, "Generate a question about the capital city");

      assertThat(
          userMessageContains(request, "Generate a question about the capital city"), is(true));
    }

    @Test
    void shouldNotIncludeNotebookAssistantInstructionsWhenEmpty() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      long systemMessageCount =
          request.messages().stream().filter(message -> message.system().isPresent()).count();
      assertThat(systemMessageCount, is(1L));
    }

    @Test
    void shouldNotIncludeRelationTypeSpecialInstructionForRegularNote() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      assertThat(
          systemMessageContains(request, "Special Instruction for Relation Note"), is(false));
    }
  }

  @Nested
  class EvaluateQuestion {
    @Test
    void shouldReturnEmptyWhenEvaluationFails() throws Exception {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);
      openAIChatCompletionMock.mockNullChatCompletion();

      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcqWithAnswer);

      assertThat(result, is(Optional.empty()));
    }

    @Test
    void shouldReturnEvaluationWhenEvaluationSucceeds() throws Exception {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      QuestionEvaluation evaluation = new QuestionEvaluation();
      evaluation.feasibleQuestion = true;
      evaluation.correctChoices = new int[] {0};
      evaluation.improvementAdvices = "Good question";
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(evaluation);

      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcqWithAnswer);

      assertThat(result.isPresent(), is(true));
      assertThat(result.get().feasibleQuestion, is(true));
      assertThat(result.get().correctChoices, equalTo(new int[] {0}));
    }
  }
}
