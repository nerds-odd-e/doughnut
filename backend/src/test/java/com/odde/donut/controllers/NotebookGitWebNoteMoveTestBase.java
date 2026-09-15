package com.odde.donut.controllers;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared fixture for slice 2 web note-move controller tests: a learned note in a Git-backed
 * notebook.
 */
abstract class NotebookGitWebNoteMoveTestBase extends NotebookGitWebContentControllerTestBase {

  static final Instant MOVE_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final Instant SAVE_AFTER_MOVE_AT = Instant.parse("2026-09-08T10:08:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String EDITED_BODY = "---\ntype: Note\n---\nlocal edit after move";

  @Autowired RelationController relationController;

  LearnedMoveFixture seedLearnedCellsInBiologyWithStudyDestination()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Biology")
            .readmeContent("Biology readme")
            .please();
    Folder study =
        makeMe.aFolder().notebook(notebook).name("Study").readmeContent("Study readme").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    snapshotCurrentPortableTree(notebook);
    return new LearnedMoveFixture(
        notebook, biology, study, cells, tracker, recallCountBefore, currentUser.getUser());
  }

  Note reloadNote(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow();
  }

  record LearnedMoveFixture(
      Notebook notebook,
      Folder biology,
      Folder study,
      Note cells,
      MemoryTracker tracker,
      long recallCountBefore,
      User owner) {}
}
