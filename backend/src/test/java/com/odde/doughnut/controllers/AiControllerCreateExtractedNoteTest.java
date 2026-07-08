package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
    void shouldPersistNewAndUpdatedNotesFromEditedFields() throws UnexpectedNoAccessRightException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      long noteCountBefore = noteRepository.count();

      NoteExtractionResult request =
          extractionResult(
              "Extracted Note",
              "Expanded content for the new note.",
              "Updated parent with summary.");
      NoteRealm response = controller.createExtractedNote(testNote, request);

      assertThat(noteRepository.count()).isEqualTo(noteCountBefore + 1);
      Note persistedNewNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedNewNote.getTitle()).isEqualTo("Extracted Note");
      assertThat(persistedNewNote.getContent()).isEqualTo("Expanded content for the new note.");
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo("Updated parent with summary.");
    }

    @Test
    void shouldRejectReservedIndexTitle() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      long noteCountBefore = noteRepository.count();
      NoteExtractionResult request =
          extractionResult("index", "New note content.", "Updated parent.");

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
  }
}
