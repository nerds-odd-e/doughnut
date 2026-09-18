package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Permanently deleting a trashed folder removes its whole subtree — nested folders, their notes and
 * each note's dependent data — while the rest of the notebook keeps its content and placement.
 */
class NotebookFolderPermanentDeleteControllerTest extends NoteDependentRowsControllerTestBase {

  @Autowired FolderRepository folderRepository;

  @Test
  void removesTheTrashedSubtreeWithoutLeavingAnyOfItsNotesAtTheNotebookRoot()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    makeMe.aNote("Root").notebook(notebook).please();
    Folder topic =
        makeMe.aFolder().inTrashOf(notebook).name("Topic").readmeContent("# topic").please();
    Folder nested = makeMe.aFolder().parentFolder(topic).name("Nested").please();
    Note cells = trackedNoteIn(topic, "Cells");
    Note nucleus = trackedNoteIn(nested, "Nucleus");
    Folder sibling =
        makeMe.aFolder().parentFolder(topic.getParentFolder()).name("Sibling").please();
    Note survivor = trackedNoteIn(sibling, "Survivor");
    makeMe.entityPersister.flushAndClear();

    controller.permanentlyDeleteFolder(reloaded(notebook), reloaded(topic));

    assertThat(folderRepository.findById(topic.getId()).isPresent(), equalTo(false));
    assertThat(folderRepository.findById(nested.getId()).isPresent(), equalTo(false));
    assertThat(noteRepository.findById(cells.getId()).isPresent(), equalTo(false));
    assertThat(noteRepository.findById(nucleus.getId()).isPresent(), equalTo(false));
    assertThat(dependentCounts(cells), equalTo(DependentCounts.allAbsent()));
    assertThat(dependentCounts(nucleus), equalTo(DependentCounts.allAbsent()));
    assertThat(rootNoteTitles(notebook), contains("Root"));
    assertThat(folderRepository.findById(sibling.getId()).isPresent(), equalTo(true));
    assertThat(reloaded(survivor).getFolder().getId(), equalTo(sibling.getId()));
    assertThat(dependentCounts(survivor).memoryTracker(), equalTo(1L));
  }

  @Test
  void removesTheTrashFolderItselfWithEverythingBelowIt() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Folder topic = makeMe.aFolder().inTrashOf(notebook).name("Topic").please();
    Folder trash = topic.getParentFolder();
    Note cells = trackedNoteIn(topic, "Cells");
    makeMe.entityPersister.flushAndClear();

    controller.permanentlyDeleteFolder(reloaded(notebook), reloaded(trash));

    assertThat(folderRepository.findById(trash.getId()).isPresent(), equalTo(false));
    assertThat(folderRepository.findById(topic.getId()).isPresent(), equalTo(false));
    assertThat(noteRepository.findById(cells.getId()).isPresent(), equalTo(false));
    assertThat(dependentCounts(cells), equalTo(DependentCounts.allAbsent()));
    assertThat(rootNoteTitles(notebook), empty());
  }

  @Test
  void removesAnEmptyTrashedFolder() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Folder empty = makeMe.aFolder().inTrashOf(notebook).name("Empty").please();

    controller.permanentlyDeleteFolder(notebook, empty);

    assertThat(folderRepository.findById(empty.getId()).isPresent(), equalTo(false));
  }

  @Test
  void refusesAFolderThatIsNotInTrashAndChangesNothing() {
    Notebook notebook = ownedNotebook();
    Folder active = ownedFolder(notebook, "Active");
    Note cells = makeMe.aNote("Cells").folder(active).please();

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.permanentlyDeleteFolder(notebook, active));

    assertThat(error.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(folderRepository.findById(active.getId()).isPresent(), equalTo(true));
    assertThat(noteRepository.findById(cells.getId()).isPresent(), equalTo(true));
  }

  private Note trackedNoteIn(Folder folder, String title) {
    Note note = makeMe.aNote(title).folder(folder).please();
    makeMe.aMemoryTrackerFor(note).please();
    return note;
  }

  /** Titles of the notebook's notes that sit at its root, with no folder of their own. */
  private List<String> rootNoteTitles(Notebook notebook) {
    return noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(notebook.getId()).stream()
        .map(Note::getTitle)
        .toList();
  }

  private Notebook reloaded(Notebook notebook) {
    return notebookRepository.findById(notebook.getId()).orElseThrow();
  }

  private Folder reloaded(Folder folder) {
    return folderRepository.findById(folder.getId()).orElseThrow();
  }

  private Note reloaded(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow();
  }
}
