package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
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
    // Initialize OpenAIChatCompletionMock
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Create common test data
    testNote = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(testNote).please();
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
      MCQWithAnswer generatedQuestion = service.generateQuestion(testNote, null);

      // Verify
      assertThat(
          generatedQuestion.getF0__multipleChoicesQuestion().getF0__stem(),
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
      service.generateQuestion(testNote, null);

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
      service.generateQuestion(testNote, null);

      // Verify
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());

      assertThat(
          paramsCaptor.getValue().model().asString(), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
    }

    @Test
    void shouldHandleNullChatCompletion() throws JsonProcessingException {
      // Mock AI response for null completion
      openAIChatCompletionMock.mockNullChatCompletion();

      // Execute and verify
      MCQWithAnswer result = service.generateQuestion(testNote, null);

      // Verify that a null completion returns null
      assertThat(result, is(nullValue()));
    }
  }

  @Nested
  class BuildQuestionGenerationRequest {

    @Test
    void shouldBuildRequestWithNoteDescription() {
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, null);
      assertThat(request, is(notNullValue()));
      assertThat(request.model().toString(), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
      boolean hasNoteDescription =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message ->
                      message.toString().contains("Focus Note and the notes related to it:"));
      assertThat("Request should contain note description", hasNoteDescription, is(true));
    }

    @Test
    void shouldBuildRequestWithNoteInstructions() {
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, null);
      assertThat(request, is(notNullValue()));
      assertThat(request.model().toString(), is(GlobalSettingsService.DEFAULT_CHAT_MODEL));
      boolean hasNoteDescription =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message ->
                      message.toString().contains("The JSON below is available only to you"));
      assertThat("Request should contain note description", hasNoteDescription, is(true));
    }

    @Test
    void shouldBuildRequestWithQuestionGenerationInstruction() {
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, null);

      boolean hasQuestionDesignerInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
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

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, null);

      boolean hasNotebookInstructions =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(message -> message.toString().contains("Custom notebook instructions"));

      assertThat(
          "Request should contain notebook assistant instructions",
          hasNotebookInstructions,
          is(true));
    }

    @Test
    void shouldIncludeAdditionalMessageWhenProvided() {
      String additionalMessage = "Generate a question about the capital city";

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, additionalMessage);

      boolean hasAdditionalMessage =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message ->
                      message.toString().contains("Generate a question about the capital city"));

      assertThat("Request should contain additional message", hasAdditionalMessage, is(true));
    }

    @Test
    void shouldNotIncludeNotebookAssistantInstructionsWhenEmpty() {
      // No NotebookAiAssistant created, so instructions should be null/empty
      // But there will be a system message with the tool instruction
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, null);

      long systemMessageCount =
          request.messages().stream().filter(message -> message.system().isPresent()).count();

      assertThat(
          "Request should have one system message with tool instruction",
          systemMessageCount,
          is(1L));
    }
  }

  @Nested
  class RelationTypeInstructions {
    @Test
    void shouldIncludeRelationTypeInstructionForRelatedTo() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.RELATED_TO).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (related to)");
                  });

      assertThat(
          "Request should contain relation type instruction for RELATED_TO",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForSpecialize() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.SPECIALIZE).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (a specialization of)");
                  });

      assertThat(
          "Request should contain relation type instruction for SPECIALIZE",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForApplication() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.APPLICATION).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (an application of)");
                  });

      assertThat(
          "Request should contain relation type instruction for APPLICATION",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForInstance() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.INSTANCE).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (an instance of)");
                  });

      assertThat(
          "Request should contain relation type instruction for INSTANCE",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForPart() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.PART).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (a part of)");
                  });

      assertThat(
          "Request should contain relation type instruction for PART",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForTaggedBy() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.TAGGED_BY).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (tagged by)");
                  });

      assertThat(
          "Request should contain relation type instruction for TAGGED_BY",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForAttribute() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.ATTRIBUTE).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (an attribute of)");
                  });

      assertThat(
          "Request should contain relation type instruction for ATTRIBUTE",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForOppositeOf() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.OPPOSITE_OF).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (the opposite of)");
                  });

      assertThat(
          "Request should contain relation type instruction for OPPOSITE_OF",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForAuthorOf() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.AUTHOR_OF).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (author of)");
                  });

      assertThat(
          "Request should contain relation type instruction for AUTHOR_OF",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForUses() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.USES).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (using)");
                  });

      assertThat(
          "Request should contain relation type instruction for USES",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForExampleOf() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.EXAMPLE_OF).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (an example of)");
                  });

      assertThat(
          "Request should contain relation type instruction for EXAMPLE_OF",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForPrecedes() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.PRECEDES).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (before)");
                  });

      assertThat(
          "Request should contain relation type instruction for PRECEDES",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForSimilarTo() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.SIMILAR_TO).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note (similar to)");
                  });

      assertThat(
          "Request should contain relation type instruction for SIMILAR_TO",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeRelationTypeInstructionForConfuseWith() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.CONFUSE_WITH).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains(
                        "Special Instruction for Relation Note (confused with)");
                  });

      assertThat(
          "Request should contain relation type instruction for CONFUSE_WITH",
          hasRelationTypeInstruction,
          is(true));
    }

    @Test
    void shouldNotIncludeRelationTypeInstructionForNonRelationshipNote() {
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(testNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note");
                  });

      assertThat(
          "Request should not contain relation type instruction for non-relationship note",
          hasRelationTypeInstruction,
          is(false));
    }
  }

  @Nested
  class NoteTypeInstructions {
    @Test
    void shouldIncludeNoteTypeInstructionForSource() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(NoteType.SOURCE);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Source Note");
                  });

      assertThat(
          "Request should contain note type instruction for SOURCE",
          hasNoteTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeNoteTypeInstructionForPerson() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(NoteType.PERSON);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Person Note");
                  });

      assertThat(
          "Request should contain note type instruction for PERSON",
          hasNoteTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeNoteTypeInstructionForConcept() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(NoteType.CONCEPT);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Concept Note");
                  });

      assertThat(
          "Request should contain note type instruction for CONCEPT",
          hasNoteTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeNoteTypeInstructionForExperience() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(NoteType.EXPERIENCE);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Experience Note");
                  });

      assertThat(
          "Request should contain note type instruction for EXPERIENCE",
          hasNoteTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeNoteTypeInstructionForInitiative() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(NoteType.INITIATIVE);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Initiative Note");
                  });

      assertThat(
          "Request should contain note type instruction for INITIATIVE",
          hasNoteTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeNoteTypeInstructionForQuest() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(NoteType.QUEST);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Quest Note");
                  });

      assertThat(
          "Request should contain note type instruction for QUEST",
          hasNoteTypeInstruction,
          is(true));
    }

    @Test
    void shouldNotIncludeNoteTypeInstructionForUnassigned() {
      Note note = makeMe.aNote().details("description long enough.").please();
      note.setNoteType(null);
      makeMe.aNote().under(note).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(note, null);

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for");
                  });

      assertThat(
          "Request should not contain note type instruction for null",
          hasNoteTypeInstruction,
          is(false));
    }

    @Test
    void shouldIncludeBothRelationTypeAndNoteTypeInstructions() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().relateTo(targetNote, RelationType.RELATED_TO).please();
      Note relationNote = sourceNote.getRelationships().get(0);
      relationNote.setNoteType(NoteType.SOURCE);
      makeMe.aNote().under(relationNote).please();

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(relationNote, null);

      boolean hasRelationTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Relation Note");
                  });

      boolean hasNoteTypeInstruction =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.system().get().content().toString();
                    return content.contains("Special Instruction for Source Note");
                  });

      assertThat(
          "Request should contain both relation type and note type instructions",
          hasRelationTypeInstruction && hasNoteTypeInstruction,
          is(true));
    }
  }

  @Nested
  class EvaluateQuestion {
    @Test
    void shouldReturnEmptyWhenEvaluationFails() throws Exception {
      // Mock AI response for question generation
      MCQWithAnswer mcqWithAnswer =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the capital of France?")
              .choices("Paris", "London", "Berlin")
              .correctChoiceIndex(0)
              .please();

      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      // Mock null response for evaluation (simulating API failure)
      openAIChatCompletionMock.mockNullChatCompletion();

      // Execute
      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcqWithAnswer);

      // Verify that evaluation failure returns empty Optional instead of throwing
      assertThat(result, is(Optional.empty()));
    }

    @Test
    void shouldReturnEvaluationWhenEvaluationSucceeds() throws Exception {
      // Mock AI response for question generation
      MCQWithAnswer mcqWithAnswer =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the capital of France?")
              .choices("Paris", "London", "Berlin")
              .correctChoiceIndex(0)
              .please();

      // Mock successful evaluation response
      QuestionEvaluation evaluation = new QuestionEvaluation();
      evaluation.feasibleQuestion = true;
      evaluation.correctChoices = new int[] {0};
      evaluation.improvementAdvices = "Good question";

      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(evaluation);

      // Execute
      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcqWithAnswer);

      // Verify that evaluation returns the result
      assertThat(result.isPresent(), is(true));
      assertThat(result.get().feasibleQuestion, is(true));
      assertThat(result.get().correctChoices, equalTo(new int[] {0}));
    }
  }
}
