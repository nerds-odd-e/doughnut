package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.sql.Timestamp;
import java.util.List;
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
        .filter(message -> message.developer().isPresent())
        .anyMatch(message -> message.developer().get().content().toString().contains(text));
  }

  private boolean userMessageContains(ChatCompletionCreateParams request, String text) {
    return request.messages().stream()
        .filter(message -> message.user().isPresent())
        .anyMatch(message -> message.toString().contains(text));
  }

  private List<String> userMessageContentStrings(ChatCompletionCreateParams request) {
    return request.messages().stream()
        .filter(message -> message.user().isPresent())
        .map(message -> message.user().get().content().toString())
        .toList();
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
          generatedQuestion.getQuestion().getQuestionStem(),
          containsString("What is the first color in the rainbow?"));
    }

    @Test
    void shouldPassScopedQuestionGenerationInstructionAsFirstUserMessage()
        throws JsonProcessingException {
      User user = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe
          .theNotebook(nb)
          .indexContent("---\nquestion_generation_instruction: SCOPED_QGEN_MARKER\n---\n")
          .please();
      Note noteInScope = makeMe.aNote().inNotebook(nb).please();
      makeMe.aNote().inNotebook(nb).please();

      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      service.generateQuestion(noteInScope, null);

      ArgumentCaptor<ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      List<String> userBodies = userMessageContentStrings(paramsCaptor.getValue());
      assertThat(
          userBodies.get(0),
          containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
      assertThat(userBodies.get(0), containsString("SCOPED_QGEN_MARKER"));
      assertThat(userBodies.get(1), containsString("# Focus Context"));
      assertThat(
          systemMessageContains(
              paramsCaptor.getValue(),
              QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER),
          is(false));
      assertThat(systemMessageContains(paramsCaptor.getValue(), "SCOPED_QGEN_MARKER"), is(false));
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
      assertThat(userMessageContains(request, "# Focus Context"), is(true));
    }

    @Test
    void shouldBuildRequestWithNoteInstructions() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      assertThat(systemMessageContains(request, "Question Designer"), is(true));
    }

    @Test
    void shouldStillIncludeFocusContextAfterDeductingInstructionTokensFromBudget() {
      User user = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe
          .theNotebook(nb)
          .indexContent("---\nquestion_generation_instruction: BUDGET_CHECK_MARKER\n---\n")
          .please();
      Note noteInScope =
          makeMe
              .aNote()
              .inNotebook(nb)
              .content("A note with enough body text to appear in the focus context.")
              .please();
      makeMe.aNote().inNotebook(nb).please();

      ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(noteInScope, null);

      List<String> userBodies = userMessageContentStrings(request);
      assertThat(
          userBodies.get(0),
          containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
      assertThat(userBodies.get(0), containsString("BUDGET_CHECK_MARKER"));
      assertThat(userBodies.get(1), containsString("# Focus Context"));
    }

    @Test
    void shouldPlaceScopedQuestionInstructionAsFirstUserMessageBeforeFocusContext() {
      User user = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe
          .theNotebook(nb)
          .indexContent("---\nquestion_generation_instruction: SCOPED_QGEN_MARKER\n---\n")
          .please();
      Note noteInScope = makeMe.aNote().inNotebook(nb).please();
      makeMe.aNote().inNotebook(nb).please();

      ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(noteInScope, null);

      List<String> userBodies = userMessageContentStrings(request);
      assertThat(
          userBodies.get(0),
          containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
      assertThat(userBodies.get(0), containsString("SCOPED_QGEN_MARKER"));
      assertThat(userBodies.get(1), containsString("# Focus Context"));
      assertThat(systemMessageContains(request, "SCOPED_QGEN_MARKER"), is(false));
      assertThat(
          systemMessageContains(
              request, QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER),
          is(false));
      assertThat(systemMessageContains(request, "focus note"), is(true));
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
    void shouldPlaceNotebookAssistantInstructionsAfterMainQuestionDesignerInstruction() {
      NotebookAiAssistant notebookAiAssistant = new NotebookAiAssistant();
      notebookAiAssistant.setNotebook(testNote.getNotebook());
      notebookAiAssistant.setAdditionalInstructionsToAi("Custom notebook instructions");
      Timestamp currentTime = new Timestamp(System.currentTimeMillis());
      notebookAiAssistant.setCreatedAt(currentTime);
      notebookAiAssistant.setUpdatedAt(currentTime);
      makeMe.entityPersister.save(notebookAiAssistant);
      makeMe.refresh(testNote.getNotebook());

      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);
      String developerBody =
          request.messages().stream()
              .filter(message -> message.developer().isPresent())
              .findFirst()
              .map(m -> m.developer().get().content().toString())
              .orElse("");

      assertThat(developerBody.indexOf("Question Designer"), greaterThan(-1));
      assertThat(
          developerBody.indexOf("Question Designer"),
          lessThan(developerBody.indexOf("Custom notebook instructions")));
    }

    @Test
    void shouldOrderUserMessagesScopedInstructionThenFocusThenAdditional() {
      User user = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
      makeMe
          .theNotebook(nb)
          .indexContent("---\nquestion_generation_instruction: SCOPED_QGEN_MARKER\n---\n")
          .please();
      Note noteInScope = makeMe.aNote().inNotebook(nb).please();
      makeMe.aNote().inNotebook(nb).please();

      ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(
              noteInScope, "Generate a question about the capital city");

      List<String> userBodies = userMessageContentStrings(request);
      assertThat(userBodies, hasSize(3));
      assertThat(
          userBodies.get(0),
          containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
      assertThat(userBodies.get(0), containsString("SCOPED_QGEN_MARKER"));
      assertThat(userBodies.get(1), containsString("# Focus Context"));
      assertThat(userBodies.get(2), containsString("Generate a question about the capital city"));
    }

    @Test
    void shouldIncludeAdditionalMessageWhenNoScopedInstruction() {
      ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(
              testNote, "Generate a question about the capital city");

      assertThat(
          userMessageContains(request, "Generate a question about the capital city"), is(true));
      List<String> userBodies = userMessageContentStrings(request);
      assertThat(userBodies.get(0), containsString("# Focus Context"));
      assertThat(userBodies.get(1), containsString("Generate a question about the capital city"));
    }

    @Test
    void shouldNotIncludeNotebookAssistantInstructionsWhenEmpty() {
      ChatCompletionCreateParams request = service.buildQuestionGenerationRequest(testNote, null);

      long systemMessageCount =
          request.messages().stream().filter(message -> message.developer().isPresent()).count();
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
