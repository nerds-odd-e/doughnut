package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
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
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebTrashControllerTest extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final Instant RECOVER_AT = Instant.parse("2026-09-08T10:05:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Autowired RelationController relationController;

  @Test
  void trashOfLearnedNestedNoteAppendsAcceptedChildWithConstructedParents() throws Exception {
    LearnedTrashFixture f = seedLearnedCellsInBiologyOnlyWithoutTrash();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));

    noteController.trashNote(f.cells(), leaveDeadLinks());

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
          containsInAnyOrder("Biology/.keep", "_trash/Biology/Cells.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "_trash/Biology/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Biology/.keep"), equalTo(""));
    }

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloaded = noteRepository.findById(f.cells().getId()).orElseThrow();
          assertThat(reloaded.isTrashed(), is(true));
          MemoryTracker learned =
              memoryTrackerRepository.findById(f.tracker().getId()).orElseThrow();
          assertThat(learned.getNote().getId(), equalTo(f.cells().getId()));
          assertThat(learned.isActive(), is(false));
          MemoryTracker removed =
              memoryTrackerRepository.findById(f.removed().getId()).orElseThrow();
          assertThat(removed.getNote().getId(), equalTo(f.cells().getId()));
          assertThat(removed.getRemovedFromTracking(), is(true));
          assertThat(
              countRecallLogsByTrackerId(f.tracker().getId()), equalTo(f.recallCountBefore()));
        });
  }

  @Test
  void ordinaryMoveAfterActualTrashAppendsAcceptedChildWithRecoveredPath() throws Exception {
    LearnedTrashFixture f = seedLearnedCellsInBiologyOnlyWithoutTrash();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    noteController.trashNote(f.cells(), leaveDeadLinks());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(RECOVER_AT));

    relationController.moveNoteToFolder(
        noteRepository.findById(f.cells().getId()).orElseThrow(), f.biology());

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
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("Biology/Cells.md", "_trash/Biology/.keep"));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          not(hasItem("_trash/Biology/Cells.md")));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Biology/Cells.md"),
          equalTo(CELLS_BODY));
    }

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloaded = noteRepository.findById(f.cells().getId()).orElseThrow();
          assertThat(reloaded.isTrashed(), is(false));
          assertThat(reloaded.getFolder().getId(), equalTo(f.biology().getId()));
          MemoryTracker learned =
              memoryTrackerRepository.findById(f.tracker().getId()).orElseThrow();
          assertThat(learned.getNote().getId(), equalTo(f.cells().getId()));
          assertThat(learned.isActive(), is(true));
          MemoryTracker removed =
              memoryTrackerRepository.findById(f.removed().getId()).orElseThrow();
          assertThat(removed.getNote().getId(), equalTo(f.cells().getId()));
          assertThat(removed.getRemovedFromTracking(), is(true));
          assertThat(
              countRecallLogsByTrackerId(f.tracker().getId()), equalTo(f.recallCountBefore()));
        });
  }

  @Test
  void undoAfterActualTrashAppendsAcceptedChildWithRecoveredPath() throws Exception {
    LearnedTrashFixture f = seedLearnedCellsInBiologyOnlyWithoutTrash();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    noteController.trashNote(f.cells(), leaveDeadLinks());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(RECOVER_AT));

    noteController.undoTrashNote(
        noteRepository.findById(f.cells().getId()).orElseThrow(), undoTo("Cells", f.biology()));

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

  LearnedTrashFixture seedLearnedCellsInBiologyOnlyWithoutTrash()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    MemoryTracker removed =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(cells.getId()).orElseThrow())
                    .spelling()
                    .removedFromTracking()
                    .please());
    long recallCountBefore =
        inCommittedTransaction(
            transactionManager, () -> countRecallLogsByTrackerId(tracker.getId()));
    snapshotCurrentPortableTree(notebook);
    return new LearnedTrashFixture(notebook, biology, cells, tracker, removed, recallCountBefore);
  }

  record LearnedTrashFixture(
      Notebook notebook,
      Folder biology,
      Note cells,
      MemoryTracker tracker,
      MemoryTracker removed,
      long recallCountBefore) {}
}
