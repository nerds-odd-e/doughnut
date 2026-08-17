package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
    void shouldRejectReservedIndexTitle() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      long noteCountBefore = noteRepository.count();

      ApiException thrown =
          assertThrows(
              ApiException.class,
              () ->
                  controller.createExtractedNote(
                      testNote,
                      extractionResult("readme", "New note content.", "Updated parent.")));

      assertThat(thrown.getErrorBody().getErrorType()).isEqualTo(ApiError.ErrorType.BINDING_ERROR);
      assertThat(thrown.getErrorBody().getErrors().get("newTitle")).contains("reserved");
      assertThat(noteRepository.count()).isEqualTo(noteCountBefore);
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(EXTRACTABLE_CONTENT);
    }

    @Test
    void shouldRejectInvalidAliasesInNewNoteContent() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());

      ApiException thrown =
          assertThrows(
              ApiException.class,
              () ->
                  controller.createExtractedNote(
                      testNote,
                      extractionResult(
                          "Extracted Note",
                          "---\naliases: color\n---\n\nbody",
                          "Updated parent with summary.")));

      assertThat(thrown.getErrorBody().getErrors().get("aliases"))
          .isEqualTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE);
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(EXTRACTABLE_CONTENT);
    }

    @Test
    void shouldStripLeadingMarkdownHeadingThatRepeatsTitleOnCreate()
        throws UnexpectedNoAccessRightException {
      Note sourceNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());

      NoteRealm response =
          controller.createExtractedNote(
              sourceNote,
              extractionResult(
                  "Key Suggestion",
                  "# Key Suggestion\n\nBody that should remain.",
                  "Updated parent with summary."));

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedNote.getContent())
          .isEqualTo("---\ntype: Note\n---\nBody that should remain.");
    }

    @Test
    void shouldCreateExtractedNoteFromSourceNote() throws UnexpectedNoAccessRightException {
      Note sourceNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      long noteCountBefore = noteRepository.count();

      NoteRealm response =
          controller.createExtractedNote(
              sourceNote,
              extractionResult(
                  "Extracted Note",
                  "Expanded content for the new note.",
                  "Updated parent with summary."));

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(noteRepository.count()).isEqualTo(noteCountBefore + 1);
      assertThat(persistedNote.getTitle()).isEqualTo("Extracted Note");
      assertThat(persistedNote.getContent())
          .isEqualTo("---\ntype: Note\n---\nExpanded content for the new note.");
      assertThat(persistedNote.getFolder()).isNull();
      makeMe.entityPersister.refresh(sourceNote);
      assertThat(sourceNote.getContent())
          .isEqualTo("---\ntype: Note\n---\nUpdated parent with summary.");
    }

    @Test
    void shouldCreateExtractedNoteInSameFolderAsSource() throws UnexpectedNoAccessRightException {
      Folder folder =
          makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("Context").please();
      Note sourceNote =
          makeMe.aNote().title("Sample").folder(folder).content(EXTRACTABLE_CONTENT).please();

      NoteRealm response =
          controller.createExtractedNote(
              sourceNote,
              extractionResult(
                  "Point B",
                  "Extracted from [[sample|the original note]].",
                  "A. See [[point b|the extracted note]]. C."));

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedNote.getFolder().getId()).isEqualTo(folder.getId());
      assertThat(response.getWikiTitles())
          .anyMatch(
              wikiTitle ->
                  wikiTitle.getTargetToken().equals("sample")
                      && wikiTitle.getDisplayText().equals("the original note")
                      && wikiTitle.getNoteId().equals(sourceNote.getId()));
    }
  }
}
