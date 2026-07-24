package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class AiControllerCreateExtractedNoteTest extends ControllerTestBase {
  @Autowired AiController controller;
  @Autowired NoteRepository noteRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class CreateExtractedNote {
    @Test
    void shouldRejectReservedIndexTitle() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      long noteCountBefore = noteRepository.count();
      NoteExtractionResult request =
          extractionResult("readme", "New note content.", "Updated parent.");

      ApiException thrown =
          assertThrows(ApiException.class, () -> controller.createExtractedNote(testNote, request));

      assertThat(thrown.getErrorBody().getErrorType()).isEqualTo(ApiError.ErrorType.BINDING_ERROR);
      assertThat(thrown.getErrorBody().getErrors().get("newTitle")).contains("reserved");
      assertThat(noteRepository.count()).isEqualTo(noteCountBefore);
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent())
          .isEqualTo("Original content with a key suggestion to extract.");
    }

    @Test
    void shouldRejectInvalidAliasesInNewNoteContent() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      String originalContent = testNote.getContent();
      NoteExtractionResult request =
          extractionResult(
              "Extracted Note", "---\naliases: color\n---\n\nbody", "Updated parent with summary.");

      ApiException thrown =
          assertThrows(ApiException.class, () -> controller.createExtractedNote(testNote, request));

      assertThat(thrown.getErrorBody().getErrorType()).isEqualTo(ApiError.ErrorType.BINDING_ERROR);
      assertThat(thrown.getErrorBody().getErrors().get("aliases"))
          .isEqualTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE);
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);
    }

    @Test
    void shouldStripLeadingMarkdownHeadingThatRepeatsTitleOnCreate()
        throws UnexpectedNoAccessRightException {
      Note sourceNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      NoteExtractionResult request =
          extractionResult(
              "Key Suggestion",
              "# Key Suggestion\n\nBody that should remain.",
              "Updated parent with summary.");

      NoteRealm response = controller.createExtractedNote(sourceNote, request);
      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();

      assertThat(persistedNote.getTitle()).isEqualTo("Key Suggestion");
      assertThat(persistedNote.getContent()).isEqualTo("Body that should remain.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldCreateExtractedNoteFromSourceNote(boolean sourceInFolder)
        throws UnexpectedNoAccessRightException {
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

      NoteExtractionResult request =
          extractionResult(
              sourceInFolder ? "Point B" : "Extracted Note",
              sourceInFolder
                  ? "Extracted from [[sample|the original note]]."
                  : "Expanded content for the new note.",
              sourceInFolder
                  ? "A. See [[point b|the extracted note]]. C."
                  : "Updated parent with summary.");
      long noteCountBefore = noteRepository.count();
      NoteRealm response = controller.createExtractedNote(sourceNote, request);
      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      if (sourceInFolder) {
        assertThat(persistedNote.getFolder().getId()).isEqualTo(expectedFolder.getId());
        assertThat(response.getWikiTitles())
            .anyMatch(
                wikiTitle ->
                    wikiTitle.getTargetToken().equals("sample")
                        && wikiTitle.getDisplayText().equals("the original note")
                        && wikiTitle.getNoteId().equals(sourceNote.getId()));
      } else {
        assertThat(noteRepository.count()).isEqualTo(noteCountBefore + 1);
        assertThat(persistedNote.getTitle()).isEqualTo("Extracted Note");
        assertThat(persistedNote.getContent()).isEqualTo("Expanded content for the new note.");
        assertThat(persistedNote.getFolder()).isNull();
        makeMe.entityPersister.refresh(sourceNote);
        assertThat(sourceNote.getContent()).isEqualTo("Updated parent with summary.");
      }
    }
  }
}
