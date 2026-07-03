package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.*;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiControllerExtractNoteTest extends ControllerTestBase {
  @Autowired AiController controller;
  @Autowired NoteRepository noteRepository;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExtractNote {
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
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

    private void stubExtractionResult(
        String newTitle, String newContent, String updatedOriginalNote) {
      NoteExtractionResult aiResult = new NoteExtractionResult();
      aiResult.setNewNoteTitle(newTitle);
      aiResult.setNewNoteContent(newContent);
      aiResult.setUpdatedOriginalNoteContent(updatedOriginalNote);
      openAiStructuredResponseMock.stubStructuredResponse(aiResult);
    }

    @Test
    void shouldCallExtractNoteWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      stubExtractionResult(
          "Extracted Note", "Expanded content for the new note.", "Updated parent with summary.");
      NoteRefinementLayout layout = sampleLayout();

      controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1-1", "p2")));

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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPlaceExtractedNoteAtExpectedLocation(boolean sourceInFolder)
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note sourceNote;
      Folder expectedFolder = null;
      if (sourceInFolder) {
        Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
        expectedFolder = makeMe.aFolder().notebook(notebook).name("Context").please();
        sourceNote =
            makeMe
                .aNote()
                .title("Sample")
                .folder(expectedFolder)
                .content("Original content with a key suggestion to extract.")
                .please();
      } else {
        sourceNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      }

      stubExtractionResult(
          sourceInFolder ? "Point B" : "Extracted Note",
          sourceInFolder ? "Extracted" : "Expanded content for the new note.",
          sourceInFolder ? "A. C. D. E." : "Updated parent with summary.");

      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");
      NoteRealm response =
          controller.extractNote(sourceNote, layoutSelectionRequest(layout, List.of("p1")));
      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      if (sourceInFolder) {
        assertThat(persistedNote.getFolder().getId()).isEqualTo(expectedFolder.getId());
      } else {
        assertThat(persistedNote.getFolder()).isNull();
        assertThat(noteRepository.findById(sourceNote.getId()).orElseThrow().getContent())
            .isEqualTo("Updated parent with summary.");
      }
    }

    @Test
    void shouldSanitizePathSeparatorsInExtractedNoteTitleAndWikiLinks()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      stubExtractionResult(
          "foo/bar: baz",
          "See [[foo/bar: baz|link]] and [[MyNb:foo/bar|nb]].",
          "Back to [[foo/bar: baz]].");
      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");

      NoteRealm response =
          controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1")));

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedNote.getTitle()).isEqualTo("foo／bar： baz");
      assertThat(persistedNote.getContent())
          .isEqualTo("See [[foo／bar： baz|link]] and [[MyNb:foo／bar|nb]].");
      assertThat(noteRepository.findById(testNote.getId()).orElseThrow().getContent())
          .isEqualTo("Back to [[foo／bar： baz]].");
    }

    @Test
    void shouldTrimSurroundingWhitespaceFromExtractedNoteTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      stubExtractionResult("\u3000Extracted Note\u3000", "Expanded content.", "Updated parent.");
      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");

      NoteRealm response =
          controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1")));

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedNote.getTitle()).isEqualTo("Extracted Note");
    }

    @Test
    void shouldRefreshWikiLinkCacheForOriginalAndNewNoteAfterExtraction()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote =
          makeMe
              .aNote()
              .title("Sample")
              .notebookOwnedBy(currentUser.getUser())
              .content("A. B. C.")
              .please();
      stubExtractionResult(
          "Point B",
          "Extracted from [[sample|the original note]].",
          "A. See [[point b|the extracted note]]. C.");
      NoteRefinementLayout layout = layoutWithItem("p1", "B");

      NoteRealm response =
          controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1")));

      assertThat(response.getWikiTitles())
          .anyMatch(
              wikiTitle ->
                  wikiTitle.getTargetToken().equals("sample")
                      && wikiTitle.getDisplayText().equals("the original note")
                      && wikiTitle.getNoteId().equals(testNote.getId()));
    }
  }
}
