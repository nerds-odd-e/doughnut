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
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.sql.Timestamp;
import java.util.Arrays;
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
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired NoteQuestionGenerationService service;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  private Note testNote;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    testNote = makeMe.aNote().please();
  }

  private boolean instructionContains(
      StructuredResponseCreateParams<MCQWithAnswer> request, String text) {
    return instructionText(request).contains(text);
  }

  private boolean userMessageContains(
      StructuredResponseCreateParams<MCQWithAnswer> request, String text) {
    return inputText(request).contains(text);
  }

  private List<String> userMessageContentStrings(
      StructuredResponseCreateParams<MCQWithAnswer> request) {
    return Arrays.asList(inputText(request).split("\n\n\n", -1));
  }

  private String instructionText(StructuredResponseCreateParams<MCQWithAnswer> request) {
    return request.rawParams().instructions().orElse("");
  }

  private String inputText(StructuredResponseCreateParams<MCQWithAnswer> request) {
    return request.rawParams().input().flatMap(input -> input.text()).orElse("");
  }

  private String modelName(StructuredResponseCreateParams<MCQWithAnswer> request) {
    return request.rawParams().model().orElseThrow().asString();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ArgumentCaptor<StructuredResponseCreateParams<MCQWithAnswer>> responseParamsCaptor() {
    return ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
  }

  @Nested
  class GenerateQuestion {
    @Test
    void shouldGenerateQuestionWithCorrectStem() throws Exception {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
      openAiStructuredResponseMock.stubStructuredResponse(jsonQuestion);

      MCQWithAnswer generatedQuestion = service.generateQuestion(testNote, null);

      assertThat(
          generatedQuestion.getQuestion().getQuestionStem(),
          containsString("What is the first color in the rainbow?"));
    }

    @Test
    void shouldUseQuestionGenerationModelNameFromGlobalSettings() throws JsonProcessingException {
      globalSettingsService
          .globalSettingQuestionGeneration()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-question-generation");
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAiStructuredResponseMock.stubStructuredResponse(mcqWithAnswer);

      service.generateQuestion(testNote, null);

      ArgumentCaptor<StructuredResponseCreateParams<MCQWithAnswer>> paramsCaptor =
          responseParamsCaptor();
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      assertThat(modelName(paramsCaptor.getValue()), is("gpt-question-generation"));
    }

    @Test
    void shouldUseSameRequestShapeAsExportedQuestionGenerationRequest()
        throws JsonProcessingException {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAiStructuredResponseMock.stubStructuredResponse(mcqWithAnswer);

      StructuredResponseCreateParams<MCQWithAnswer> exportedRequest =
          service.buildQuestionGenerationRequest(testNote, "Generate a focused question");

      service.generateQuestion(testNote, "Generate a focused question");

      ArgumentCaptor<StructuredResponseCreateParams<MCQWithAnswer>> paramsCaptor =
          responseParamsCaptor();
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<MCQWithAnswer> runtimeRequest = paramsCaptor.getValue();
      assertThat(modelName(runtimeRequest), is(modelName(exportedRequest)));
      assertThat(inputText(runtimeRequest), is(inputText(exportedRequest)));
      assertThat(instructionText(runtimeRequest), is(instructionText(exportedRequest)));
    }

    @Test
    void shouldReturnNullWhenStructuredResponseIsAbsent() throws JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(null);

      MCQWithAnswer result = service.generateQuestion(testNote, null);

      assertThat(result, is(nullValue()));
    }
  }

  private Note noteWithScopedQuestionGenerationInstruction(String marker) {
    User user = makeMe.aUser().please();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
    makeMe
        .theNotebook(nb)
        .indexContent("---\nquestion_generation_instruction: " + marker + "\n---\n")
        .please();
    return makeMe
        .aNote()
        .notebook(nb)
        .content("Note body text included in the focus context.")
        .please();
  }

  @Nested
  class BuildQuestionGenerationRequest {

    @Test
    void shouldBuildRequestWithNoteDescription() {
      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);

      assertThat(request, is(notNullValue()));
      assertThat(modelName(request), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
      assertThat(userMessageContains(request, "# Focus Context"), is(true));
    }

    @Test
    void shouldBuildRequestWithNoteInstructions() {
      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);

      assertThat(instructionContains(request, "Question Designer"), is(true));
    }

    @Test
    void shouldPlaceScopedQuestionInstructionAsFirstUserMessageBeforeFocusContext() {
      Note noteInScope = noteWithScopedQuestionGenerationInstruction("SCOPED_QGEN_MARKER");

      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(noteInScope, null);

      List<String> userBodies = userMessageContentStrings(request);
      assertThat(
          userBodies.get(0),
          containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
      assertThat(userBodies.get(0), containsString("SCOPED_QGEN_MARKER"));
      assertThat(userBodies.get(1), containsString("# Focus Context"));
      assertThat(instructionContains(request, "SCOPED_QGEN_MARKER"), is(false));
      assertThat(
          instructionContains(
              request, QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER),
          is(false));
      assertThat(instructionContains(request, "focus note"), is(true));
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

      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);

      assertThat(instructionContains(request, "Custom notebook instructions"), is(true));
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

      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);
      String developerBody = instructionText(request);

      assertThat(developerBody.indexOf("Question Designer"), greaterThan(-1));
      assertThat(
          developerBody.indexOf("Question Designer"),
          lessThan(developerBody.indexOf("Custom notebook instructions")));
    }

    @Test
    void shouldOrderUserMessagesScopedInstructionThenFocusThenAdditional() {
      Note noteInScope = noteWithScopedQuestionGenerationInstruction("SCOPED_QGEN_MARKER");

      StructuredResponseCreateParams<MCQWithAnswer> request =
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
      StructuredResponseCreateParams<MCQWithAnswer> request =
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
      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);

      assertThat(instructionText(request), is(not(emptyString())));
    }

    @Test
    void shouldNotIncludeRelationTypeSpecialInstructionForRegularNote() {
      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);

      assertThat(instructionContains(request, "Special Instruction for Relation Note"), is(false));
    }
  }

  @Nested
  class EvaluateQuestion {
    @Test
    void shouldReturnEmptyWhenEvaluationFails() throws Exception {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAiStructuredResponseMock.stubStructuredResponse(null);

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
      openAiStructuredResponseMock.stubStructuredResponse(evaluation);

      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcqWithAnswer);

      assertThat(result.isPresent(), is(true));
      assertThat(result.get().feasibleQuestion, is(true));
      assertThat(result.get().correctChoices, equalTo(new int[] {0}));
    }
  }
}
