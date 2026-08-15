package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.focusContext.FocusContextConstants;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteQuestionGenerationRequestTests extends NoteQuestionGenerationServiceTestBase {

  @Autowired StructuredResponseCreateParamsSerializer paramsSerializer;

  @Test
  void shouldBuildRequestWithNoteDescription() {
    StructuredResponseCreateParams<GeneratedMcq> request =
        service.buildQuestionGenerationRequest(testNote, null);

    assertThat(request, is(notNullValue()));
    assertThat(modelName(request), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
    assertThat(
        userMessageContains(request, FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER), is(true));
  }

  @Test
  void shouldBuildRequestWithNoteInstructions() {
    assertThat(
        instructionContains(
            service.buildQuestionGenerationRequest(testNote, null), "Question Designer"),
        is(true));
  }

  @Test
  void shouldPlaceScopedQuestionInstructionAsFirstUserMessageBeforeFocusContext() {
    Note noteInScope = noteWithQuestionGenerationInstructions("SCOPED_QGEN_MARKER", null);

    StructuredResponseCreateParams<GeneratedMcq> request =
        service.buildQuestionGenerationRequest(noteInScope, null);

    List<String> userBodies = userMessageContentStrings(request);
    assertThat(
        userBodies.get(0),
        containsString(QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER));
    assertThat(userBodies.get(0), containsString("SCOPED_QGEN_MARKER"));
    assertThat(userBodies.get(1), containsString(FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER));
    assertThat(instructionContains(request, "SCOPED_QGEN_MARKER"), is(false));
    assertThat(
        instructionContains(
            request, QuestionGenerationRequestBuilder.CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER),
        is(false));
    assertThat(instructionContains(request, "focus note"), is(true));
  }

  @Test
  void nestedLabeledQuestionInstructionsAppearInFirstUserMessageInOrder() {
    User user = makeMe.aUser().please();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(user).name("Physics").please();
    makeMe
        .theNotebook(nb)
        .readmeContent("---\nquestion_generation_instruction: NOTEBOOK_INSTRUCTION\n---\n")
        .please();
    Folder outer = makeMe.aFolder().notebook(nb).name("Mechanics").please();
    makeMe
        .theFolder(outer)
        .readmeContent("---\nquestion_generation_instruction: OUTER_INSTRUCTION\n---\n")
        .please();
    Folder inner = makeMe.aFolder().parentFolder(outer).name("Kinematics").please();
    makeMe
        .theFolder(inner)
        .readmeContent("---\nquestion_generation_instruction: INNER_INSTRUCTION\n---\n")
        .please();
    Note note =
        makeMe
            .aNote()
            .folder(inner)
            .content("---\nquestion_generation_instruction: NOTE_INSTRUCTION\n---\nBody")
            .please();

    StructuredResponseCreateParams<GeneratedMcq> request =
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
  void shouldOrderUserMessagesScopedInstructionThenFocusThenAdditional() {
    Note noteInScope = noteWithQuestionGenerationInstructions("SCOPED_QGEN_MARKER", null);

    List<String> userBodies =
        userMessageContentStrings(
            service.buildQuestionGenerationRequest(
                noteInScope, "Generate a question about the capital city"));

    assertThat(userBodies, hasSize(3));
    assertThat(userBodies.get(2), containsString("Generate a question about the capital city"));
  }

  @Test
  void shouldIncludeAdditionalMessageWhenNoScopedInstruction() {
    StructuredResponseCreateParams<GeneratedMcq> request =
        service.buildQuestionGenerationRequest(
            testNote, "Generate a question about the capital city");

    assertThat(
        userMessageContains(request, "Generate a question about the capital city"), is(true));
    List<String> userBodies = userMessageContentStrings(request);
    assertThat(userBodies.get(0), containsString(FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER));
    assertThat(userBodies.get(1), containsString("Generate a question about the capital city"));
  }

  @Test
  void shouldNotIncludeRelationTypeSpecialInstructionForRegularNote() {
    StructuredResponseCreateParams<GeneratedMcq> request =
        service.buildQuestionGenerationRequest(testNote, null);

    assertThat(instructionContains(request, "Special Instruction for Relation Note"), is(false));
  }

  @Test
  void omitsReasoningForNonReasoningModel() {
    StructuredResponseCreateParams<GeneratedMcq> request =
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

    StructuredResponseCreateParams<GeneratedMcq> request =
        service.buildQuestionGenerationRequest(testNote, null);
    Map<String, Object> body = paramsSerializer.toBodyMap(request);

    assertThat(body.get("max_output_tokens"), is(2000));
    @SuppressWarnings("unchecked")
    Map<String, Object> reasoning = (Map<String, Object>) body.get("reasoning");
    assertThat(reasoning, is(notNullValue()));
    assertThat(reasoning.get("effort"), is("medium"));
  }

  private boolean instructionContains(
      StructuredResponseCreateParams<GeneratedMcq> request, String text) {
    return instructionText(request).contains(text);
  }

  private boolean userMessageContains(
      StructuredResponseCreateParams<GeneratedMcq> request, String text) {
    return inputText(request).contains(text);
  }

  private List<String> userMessageContentStrings(
      StructuredResponseCreateParams<GeneratedMcq> request) {
    return Arrays.asList(inputText(request).split("\n\n\n", -1));
  }

  private Note noteWithQuestionGenerationInstructions(
      String containerInstruction, String noteInstruction) {
    User user = makeMe.aUser().please();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(user).please();
    if (containerInstruction != null) {
      makeMe
          .theNotebook(nb)
          .readmeContent(
              "---\nquestion_generation_instruction: " + containerInstruction + "\n---\n")
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
}
