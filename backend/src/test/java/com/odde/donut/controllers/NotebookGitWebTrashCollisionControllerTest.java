package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

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
import org.junit.jupiter.api.Test;

class NotebookGitWebTrashCollisionControllerTest extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final String EARLIER_CELLS_BODY = "---\ntype: Note\n---\nearlier cells";
  static final String CELLS_THREE_BODY = "---\ntype: Note\n---\ncells three";
  static final String ACTIVE_CELLS_BODY = "---\ntype: Note\n---\nactive cells";

  @Test
  void trashOfActiveCellsUsesFirstFreeTrashTitleWithoutChangingEarlierTrashBytes()
      throws Exception {
    CollisionTrashFixture f = seedActiveCellsWithEarlierTrashGap();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));

    noteController.trashNote(f.activeCells(), leaveDeadLinks());

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder(
              "Biology/.keep",
              "_trash/Biology/Cells.md",
              "_trash/Biology/Cells (2).md",
              "_trash/Biology/Cells (3).md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "_trash/Biology/Cells.md"),
          equalTo(EARLIER_CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "_trash/Biology/Cells (3).md"),
          equalTo(CELLS_THREE_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "_trash/Biology/Cells (2).md"),
          equalTo(ACTIVE_CELLS_BODY));
    }
  }

  CollisionTrashFixture seedActiveCellsWithEarlierTrashGap()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note activeCells = makeMe.aNote("Cells").folder(biology).content(ACTIVE_CELLS_BODY).please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashBiology = makeMe.aFolder().parentFolder(trash).name("Biology").please();
    makeMe.aNote("Cells").folder(trashBiology).content(EARLIER_CELLS_BODY).please();
    makeMe.aNote("Cells (3)").folder(trashBiology).content(CELLS_THREE_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new CollisionTrashFixture(notebook, activeCells);
  }

  record CollisionTrashFixture(Notebook notebook, Note activeCells) {}
}
