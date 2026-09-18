package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Permanently deleting a trashed note removes it with its complete dependent data, while the rest
 * of the notebook — the peer in its folder and the authored text of a note linking to it — stays.
 */
class NoteControllerPermanentDeleteTests extends NoteDependentRowsControllerTestBase {
  private static final String REFERRER_CONTENT =
      "---\ntype: Note\nexample of: \"[[Cells]]\"\n---\nBody [[Cells]]\n";

  @Autowired NoteController noteController;

  @Test
  void removesTheTrashedNoteWithItsDependentDataAndKeepsTheRestOfTheNotebook()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Folder trashedBiology = makeMe.aFolder().inTrashOf(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(trashedBiology).please();
    makeMe
        .aRecallPrompt()
        .forMemoryTracker(makeMe.aMemoryTrackerFor(cells).please())
        .withMcqForNote(cells)
        .please();
    makeMe.anImage().forNote(cells).please();
    makeMe.aConversationMessage(makeMe.aConversation().forANote(cells).please()).please();
    Note peer = makeMe.aNote("Atoms").folder(trashedBiology).please();
    Note referrer = makeMe.aNote("Referrer").underSameNotebookAs(cells).please();
    authorReferencingContent(referrer, REFERRER_CONTENT);
    makeMe.entityPersister.flushAndClear();

    noteController.permanentlyDeleteNote(reload(cells.getId()));

    assertThat(noteRepository.findById(cells.getId()).isPresent(), equalTo(false));
    assertThat(dependentCounts(cells), equalTo(DependentCounts.allAbsent()));
    assertThat(noteRepository.findById(peer.getId()).isPresent(), equalTo(true));
    assertThat(reload(referrer.getId()).getContent(), equalTo(REFERRER_CONTENT));
  }

  @Test
  void refusesToPermanentlyDeleteANoteThatIsNotInTrash() {
    Note active = makeMe.aNote("Active").notebookOwnedBy(currentUser.getUser()).please();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> noteController.permanentlyDeleteNote(active));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(noteRepository.findById(active.getId()).isPresent(), equalTo(true));
  }

  @Test
  void rejectsPermanentDeleteOfAnotherOwnersTrashedNote() {
    Note note =
        makeMe
            .aNote("Cells")
            .notebook(makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please())
            .trashed()
            .please();

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> noteController.permanentlyDeleteNote(note));
  }

  private Note reload(Integer noteId) {
    return noteRepository.findById(noteId).orElseThrow();
  }
}
