package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

/**
 * Slice 3 root/nested placement variants: a root note moves into an existing nested folder, and a
 * folder note moves to the notebook root through both root endpoint spellings. Each variant asserts
 * only its delta (changed path/parent and accepted-tree) on top of slice 2's canonical identity
 * shape; cross-notebook endpoint regression is preserved by the full backend run.
 */
class NotebookGitWebNoteMoveRootNestedControllerTest extends NotebookGitWebNoteMoveTestBase {

  @Test
  void rootNoteMovesIntoNestedFolder_appendsAcceptedTreeAtNestedPathRootPathGone()
      throws UnexpectedNoAccessRightException, Exception {
    LearnedMoveFixture f = seedLearnedCellsAtRootWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    relationController.moveNoteToFolder(f.cells(), f.study());

    assertThat(reloadNote(f.cells()).getFolder().getId(), equalTo(f.study().getId()));
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(downloadedHead, equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitB = revWalk.parseCommit(downloadedHead);
        assertThat(commitB.getParentCount(), is(1));
        assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder(
              "README.md", "Biology/README.md", "Study/README.md", "Study/Cells.md"));
      assertThat(GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Cells.md")));
    }
  }

  @Test
  void nestedFolderNoteMovesToRootViaBareRootEndpoint_appendsAcceptedTreeAtRootFolderPathGone()
      throws UnexpectedNoAccessRightException, Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    relationController.moveNoteToNotebookRoot(f.cells());

    assertThat(reloadNote(f.cells()).getFolder(), nullValue());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(downloadedHead, equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitB = revWalk.parseCommit(downloadedHead);
        assertThat(commitB.getParentCount(), is(1));
        assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("Biology/README.md", "Study/README.md", "Cells.md"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Biology/Cells.md")));
    }
  }

  @Test
  void
      nestedFolderNoteMovesToRootViaExplicitTargetNotebookEndpoint_appendsAcceptedTreeAtRootFolderPathGone()
          throws UnexpectedNoAccessRightException, Exception {
    LearnedMoveFixture f = seedLearnedCellsInBiologyWithStudyDestination();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(MOVE_AT));

    relationController.moveNoteToNotebookRootInNotebook(f.cells(), f.notebook());

    assertThat(reloadNote(f.cells()).getFolder(), nullValue());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(downloadedHead, equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit commitB = revWalk.parseCommit(downloadedHead);
        assertThat(commitB.getParentCount(), is(1));
        assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("Biology/README.md", "Study/README.md", "Cells.md"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), not(hasItem("Biology/Cells.md")));
    }
  }

  /**
   * A learned note at the notebook root with an existing nested {@code Study} folder and a {@code
   * Biology} folder, both with READMEs — the root-to-nested placement fixture. The notebook README
   * seeds {@code README.md} so the root path is observable in the accepted tree.
   */
  LearnedMoveFixture seedLearnedCellsAtRootWithStudyDestination()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    makeMe.theNotebook(notebook).readmeContent("Notebook landing").please();
    Folder biology =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Biology")
            .readmeContent("Biology readme")
            .please();
    Folder study =
        makeMe.aFolder().notebook(notebook).name("Study").readmeContent("Study readme").please();
    Note cells = makeMe.aNote("Cells").notebook(notebook).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    long recallCountBefore = countRecallPromptsByNoteId(cells.getId());
    snapshotCurrentPortableTree(notebook);
    return new LearnedMoveFixture(
        notebook, biology, study, cells, tracker, recallCountBefore, currentUser.getUser());
  }
}
