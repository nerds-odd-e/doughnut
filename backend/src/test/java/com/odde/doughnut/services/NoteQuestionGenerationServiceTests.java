package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
  @Autowired StructuredResponseCreateParamsSerializer paramsSerializer;
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

  private Note noteWithQuestionGenerationInstructions(
      String containerInstruction, String noteInstruction) {
    User user = makeMe.aUser().please();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
    if (containerInstruction != null) {
      makeMe
          .theNotebook(nb)
          .indexContent("---\nquestion_generation_instruction: " + containerInstruction + "\n---\n")
          .please();
    }
    String content =
        noteInstruction != null
            ? "---\nquestion_generation_instruction: "
                + noteInstruction
                + "\n---\nNote body text included in the focus context."
            : "Note body text included in the focus context.";
    return makeMe.aNote().notebook(nb).content(content).please();
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
      Note noteInScope = noteWithQuestionGenerationInstructions("SCOPED_QGEN_MARKER", null);

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
    void shouldPlaceContainerThenNoteQuestionInstructionsInFirstUserMessage() {
      User user = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(user).name("Physics").please();
      makeMe
          .theNotebook(nb)
          .indexContent("---\nquestion_generation_instruction: NOTEBOOK_INSTRUCTION\n---\n")
          .please();
      Folder outer = makeMe.aFolder().notebook(nb).name("Mechanics").please();
      makeMe
          .theFolder(outer)
          .indexContent("---\nquestion_generation_instruction: OUTER_INSTRUCTION\n---\n")
          .please();
      Folder inner = makeMe.aFolder().parentFolder(outer).name("Kinematics").please();
      makeMe
          .theFolder(inner)
          .indexContent("---\nquestion_generation_instruction: INNER_INSTRUCTION\n---\n")
          .please();
      Note note =
          makeMe
              .aNote()
              .folder(inner)
              .content("---\nquestion_generation_instruction: NOTE_INSTRUCTION\n---\nBody")
              .please();

      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(note, null);

      List<String> userBodies = userMessageContentStrings(request);
      String instructionMessage = userBodies.get(0);
      assertThat(
          instructionMessage,
          containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
      assertThat(instructionMessage, containsString("Instruction from notebook \"Physics\":"));
      assertThat(instructionMessage, containsString("NOTEBOOK_INSTRUCTION"));
      assertThat(instructionMessage, containsString("Instruction from folder \"Mechanics\":"));
      assertThat(instructionMessage, containsString("OUTER_INSTRUCTION"));
      assertThat(instructionMessage, containsString("Instruction from folder \"Kinematics\":"));
      assertThat(instructionMessage, containsString("INNER_INSTRUCTION"));
      assertThat(instructionMessage, containsString("Instruction from the focus note:"));
      assertThat(instructionMessage, containsString("NOTE_INSTRUCTION"));
      assertThat(
          instructionMessage.indexOf("NOTEBOOK_INSTRUCTION"),
          lessThan(instructionMessage.indexOf("OUTER_INSTRUCTION")));
      assertThat(
          instructionMessage.indexOf("OUTER_INSTRUCTION"),
          lessThan(instructionMessage.indexOf("INNER_INSTRUCTION")));
      assertThat(
          instructionMessage.indexOf("INNER_INSTRUCTION"),
          lessThan(instructionMessage.indexOf("NOTE_INSTRUCTION")));
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
      Note noteInScope = noteWithQuestionGenerationInstructions("SCOPED_QGEN_MARKER", null);

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

    @Test
    void omitsReasoningForNonReasoningModel() {
      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);
      Map<String, Object> body = paramsSerializer.toBodyMap(request);

      assertThat(body.containsKey("reasoning"), is(false));
      assertThat(body.get("max_output_tokens"), is(1000));
    }

    @Test
    void usesMediumReasoningForReasoningModel() {
      globalSettingsService
          .globalSettingQuestionGeneration()
          .setKeyValue(makeMe.aTimestamp().please(), "o3-mini");

      StructuredResponseCreateParams<MCQWithAnswer> request =
          service.buildQuestionGenerationRequest(testNote, null);
      Map<String, Object> body = paramsSerializer.toBodyMap(request);

      assertThat(body.get("max_output_tokens"), is(2000));
      @SuppressWarnings("unchecked")
      Map<String, Object> reasoning = (Map<String, Object>) body.get("reasoning");
      assertThat(reasoning, is(notNullValue()));
      assertThat(reasoning.get("effort"), is("medium"));
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
