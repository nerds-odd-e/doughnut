package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
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
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(null);

      assertThat(request, is(notNullValue()));
      assertThat(request.model().toString(), is("gpt-4o-mini"));
      boolean hasNoteDescription =
          request.messages().stream()
              .filter(message -> message.system().isPresent())
              .anyMatch(
                  message ->
                      message.toString().contains("Focus Note and the notes related to it:"));
      assertThat("Request should contain note description", hasNoteDescription, is(true));
    }

    @Test
    void shouldBuildRequestWithQuestionGenerationInstruction() {
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(null);

      boolean hasQuestionDesignerInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
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

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(null);

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
          service.buildQuestionGenerationRequest(additionalMessage);

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
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(null);

      long systemMessageCount =
          request.messages().stream().filter(message -> message.system().isPresent()).count();

      assertThat(
          "Request should have only one system message (note description)",
          systemMessageCount,
          is(1L));
    }
  }

  @Nested
  class LinkTypeInstructions {
    @Test
    void shouldIncludeLinkTypeInstructionForRelatedTo() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.RELATED_TO).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (related to)");
                  });

      assertThat(
          "Request should contain link type instruction for RELATED_TO",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForSpecialize() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.SPECIALIZE).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains(
                        "Special Instruction for Linking Note (a specialization of)");
                  });

      assertThat(
          "Request should contain link type instruction for SPECIALIZE",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForApplication() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.APPLICATION).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains(
                        "Special Instruction for Linking Note (an application of)");
                  });

      assertThat(
          "Request should contain link type instruction for APPLICATION",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForInstance() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.INSTANCE).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains(
                        "Special Instruction for Linking Note (an instance of)");
                  });

      assertThat(
          "Request should contain link type instruction for INSTANCE",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForPart() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.PART).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (a part of)");
                  });

      assertThat(
          "Request should contain link type instruction for PART",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForTaggedBy() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.TAGGED_BY).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (tagged by)");
                  });

      assertThat(
          "Request should contain link type instruction for TAGGED_BY",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForAttribute() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.ATTRIBUTE).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains(
                        "Special Instruction for Linking Note (an attribute of)");
                  });

      assertThat(
          "Request should contain link type instruction for ATTRIBUTE",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForOppositeOf() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.OPPOSITE_OF).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains(
                        "Special Instruction for Linking Note (the opposite of)");
                  });

      assertThat(
          "Request should contain link type instruction for OPPOSITE_OF",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForAuthorOf() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.AUTHOR_OF).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (author of)");
                  });

      assertThat(
          "Request should contain link type instruction for AUTHOR_OF",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForUses() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.USES).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (using)");
                  });

      assertThat(
          "Request should contain link type instruction for USES",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForExampleOf() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.EXAMPLE_OF).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (an example of)");
                  });

      assertThat(
          "Request should contain link type instruction for EXAMPLE_OF",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForPrecedes() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.PRECEDES).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (before)");
                  });

      assertThat(
          "Request should contain link type instruction for PRECEDES",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForSimilarTo() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.SIMILAR_TO).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (similar to)");
                  });

      assertThat(
          "Request should contain link type instruction for SIMILAR_TO",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldIncludeLinkTypeInstructionForConfuseWith() {
      Note targetNote = makeMe.aNote().please();
      Note sourceNote = makeMe.aNote().linkTo(targetNote, LinkType.CONFUSE_WITH).please();
      Note linkingNote = sourceNote.getLinks().get(0);
      makeMe.aNote().under(linkingNote).please();
      NoteQuestionGenerationService linkService =
          notebookAssistantForNoteServiceFactory.createNoteQuestionGenerationService(linkingNote);

      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          linkService.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note (confused with)");
                  });

      assertThat(
          "Request should contain link type instruction for CONFUSE_WITH",
          hasLinkTypeInstruction,
          is(true));
    }

    @Test
    void shouldNotIncludeLinkTypeInstructionForNonLinkingNote() {
      com.openai.models.chat.completions.ChatCompletionCreateParams request =
          service.buildQuestionGenerationRequest(null);

      boolean hasLinkTypeInstruction =
          request.messages().stream()
              .filter(message -> message.user().isPresent())
              .anyMatch(
                  message -> {
                    String content = message.user().get().content().toString();
                    return content.contains("Special Instruction for Linking Note");
                  });

      assertThat(
          "Request should not contain link type instruction for non-linking note",
          hasLinkTypeInstruction,
          is(false));
    }
  }
}
