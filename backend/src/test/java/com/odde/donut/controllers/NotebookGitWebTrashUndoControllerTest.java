package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitWebTrashUndoControllerTest extends NotebookGitWebContentControllerTestBase {
  static final Instant UNDO_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Test
  void undoOfAlreadyRepresentedTrashAppendsAcceptedChildWithRecoveredPath() throws Exception {
    AlreadyTrashedFixture f = seedAlreadyRepresentedTrashUnderBiology();
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(UNDO_AT));

    noteController.undoTrashNote(f.cells(), undoTo("Cells", f.biology()));

    ObjectId acceptedC = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(downloadedHead, equalTo(acceptedC));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitC = revWalk.parseCommit(downloadedHead);
        assertThat(commitC.getParentCount(), is(1));
        assertThat(commitC.getParent(0).getId(), equalTo(acceptedB));
      }
      assertThat(GitBundleTestReader.pathsIn(repo, downloadedHead), hasItem("Biology/Cells.md"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          not(hasItem("_trash/Biology/Cells.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Biology/Cells.md"),
          equalTo(CELLS_BODY));
    }
  }

  AlreadyTrashedFixture seedAlreadyRepresentedTrashUnderBiology()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Biology")
            .readmeContent("Biology readme")
            .please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashBiology = makeMe.aFolder().parentFolder(trash).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(trashBiology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new AlreadyTrashedFixture(notebook, biology, cells);
  }

  record AlreadyTrashedFixture(Notebook notebook, Folder biology, Note cells) {}
}
