package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiControllerExtractNotePreviewTest extends ControllerTestBase {
  @Autowired AiController controller;
  @Autowired NoteRepository noteRepository;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExtractNotePreview {
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    }

    @Test
    void shouldReturnExtractionPreviewWithoutPersisting()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      String originalContent = testNote.getContent();
      long noteCountBefore = noteRepository.count();

      openAiStructuredResponseMock.stubStructuredResponse(
          extractionResult(
              "Extracted Note",
              "Expanded content for the new note.",
              "Updated parent with summary."));

      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");
      NoteExtractionResult response =
          controller.extractNotePreview(testNote, layoutSelectionRequest(layout, List.of("p1")));

      assertThat(response.getNewNoteTitle()).isEqualTo("Extracted Note");
      assertThat(response.getNewNoteContent()).isEqualTo("Expanded content for the new note.");
      assertThat(response.getUpdatedOriginalNoteContent())
          .isEqualTo("Updated parent with summary.");
      assertThat(noteRepository.count()).isEqualTo(noteCountBefore);
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);
    }

    private NoteRefinementLayout sampleLayout() {
      return new NoteRefinementLayout(
          List.of(
              new NoteRefinementLayoutItem(
                  "p1",
                  "Main concept",
                  false,
                  List.of(
                      new NoteRefinementLayoutItem(
                          "p1-1", "key suggestion to extract", false, List.of()))),
              new NoteRefinementLayoutItem("p2", "Other point", false, List.of())));
    }

    @Test
    void shouldCallExtractNoteWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      openAiStructuredResponseMock.stubStructuredResponse(
          extractionResult(
              "Extracted Note",
              "Expanded content for the new note.",
              "Updated parent with summary."));
      NoteRefinementLayout layout = sampleLayout();

      controller.extractNotePreview(
          testNote, layoutSelectionRequest(layout, List.of("p1-1", "p2")));

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteExtractionResult>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteExtractionResult> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(3000L));
      assertThat(instructions).contains("Full note layout:");
      assertThat(instructions).contains("\"id\" : \"p1-1\"");
      assertThat(instructions).contains("Selected layout item ids to extract together");
      assertThat(instructions).contains("[p1-1, p2]");
      assertThat(instructions).contains("- p1-1: \"key suggestion to extract\"");
      assertThat(instructions).contains("- p2: \"Other point\"");
      assertThat(instructions)
          .contains(
              "Prefer replacing the removed content in the original note with a natural contextual wiki link to the new note");
      assertThat(instructions)
          .contains(
              "Do not add YAML frontmatter or metadata properties, such as parent:, merely to backlink the new note to the original note");
      assertThat(instructions)
          .contains("Never use a generic parent property as the default extraction relationship");
      assertThat(instructions).contains("Wiki links are case-insensitive");
      assertThat(instructions).contains("[[Canonical Note Title|visible text]]");
      assertThat(instructions).contains("alreadyExtracted");
    }

    @Test
    void shouldSanitizePathSeparatorsInExtractionPreview()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      openAiStructuredResponseMock.stubStructuredResponse(
          extractionResult(
              "foo/bar: baz",
              "See [[foo/bar: baz|link]] and [[MyNb:foo/bar|nb]].",
              "Back to [[foo/bar: baz]]."));
      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");

      NoteExtractionResult response =
          controller.extractNotePreview(testNote, layoutSelectionRequest(layout, List.of("p1")));

      assertThat(response.getNewNoteTitle()).isEqualTo("foo／bar： baz");
      assertThat(response.getNewNoteContent())
          .isEqualTo("See [[foo／bar： baz|link]] and [[MyNb:foo／bar|nb]].");
      assertThat(response.getUpdatedOriginalNoteContent()).isEqualTo("Back to [[foo／bar： baz]].");
    }

    @Test
    void shouldTrimSurroundingWhitespaceFromExtractionPreviewTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      openAiStructuredResponseMock.stubStructuredResponse(
          extractionResult("\u3000Extracted Note\u3000", "Expanded content.", "Updated parent."));
      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");

      NoteExtractionResult response =
          controller.extractNotePreview(testNote, layoutSelectionRequest(layout, List.of("p1")));

      assertThat(response.getNewNoteTitle()).isEqualTo("Extracted Note");
    }
  }
}
