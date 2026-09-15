package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
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

/**
 * A web content save queued behind Trash on the same notebook appends both accepted revisions in
 * parent order, and both results survive in the final tree.
 */
class NotebookGitWebTrashQueuedWriterControllerTest
    extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final Instant SAVE_AFTER_TRASH_AT = Instant.parse("2026-09-08T10:08:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String ATOMS_BODY = "---\ntype: Note\n---\natoms body";
  static final String EDITED_ATOMS_BODY = "---\ntype: Note\n---\nedited after trash";

  @Test
  void queuedSaveAfterTrashAppendsBothRevisionsInParentOrder() throws Exception {
    QueuedWriterFixture f = seedCellsInBiologyAndAtomsInChemistry();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());

    NotebookGitConcurrentWriterTestSupport.Result<NoteRealm, NoteRealm> race =
        NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
            transactionManager,
            notebookGitBindingRepository,
            currentUser,
            currentUser.getUser(),
            f.notebook().getId(),
            () -> {
              testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
              return noteController.trashNote(
                  noteRepository.findById(f.cells().getId()).orElseThrow(), leaveDeadLinks());
            },
            () -> {
              testabilitySettings.timeTravelTo(Timestamp.from(SAVE_AFTER_TRASH_AT));
              return textContentController.updateNoteContent(
                  noteRepository.findById(f.atoms().getId()).orElseThrow(),
                  contentDto(EDITED_ATOMS_BODY));
            });

    assertThat(race.first().getNote().isTrashed(), is(true));
    assertThat(race.first().getNote().getFolder().getParentFolder().getName(), equalTo("_trash"));
    assertThat(race.second().getNote().getContent(), equalTo(EDITED_ATOMS_BODY));
    byte[] bundleBytes =
        inCommittedTransaction(transactionManager, () -> binding(f.notebook()).getBundleBytes());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repo, bundleBytes);
      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit saveCommit = revWalk.parseCommit(head);
        assertThat(saveCommit.getParentCount(), is(1));
        RevCommit trashCommit = revWalk.parseCommit(saveCommit.getParent(0));
        assertThat(trashCommit.getParentCount(), is(1));
        assertThat(trashCommit.getParent(0).getId(), equalTo(acceptedA));
        assertThat(
            GitBundleTestReader.pathsIn(repo, saveCommit),
            containsInAnyOrder("Biology/.keep", "Chemistry/Atoms.md", "_trash/Biology/Cells.md"));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repo, saveCommit, "_trash/Biology/Cells.md"),
            equalTo(CELLS_BODY));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repo, saveCommit, "Chemistry/Atoms.md"),
            equalTo(EDITED_ATOMS_BODY));
        assertThat(
            GitBundleTestReader.pathsIn(repo, trashCommit),
            containsInAnyOrder("Biology/.keep", "Chemistry/Atoms.md", "_trash/Biology/Cells.md"));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repo, trashCommit, "_trash/Biology/Cells.md"),
            equalTo(CELLS_BODY));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repo, trashCommit, "Chemistry/Atoms.md"),
            equalTo(ATOMS_BODY));
      }
    }
  }

  QueuedWriterFixture seedCellsInBiologyAndAtomsInChemistry()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Folder chemistry = makeMe.aFolder().notebook(notebook).name("Chemistry").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note atoms = makeMe.aNote("Atoms").folder(chemistry).content(ATOMS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new QueuedWriterFixture(notebook, cells, atoms);
  }

  record QueuedWriterFixture(Notebook notebook, Note cells, Note atoms) {}
}
