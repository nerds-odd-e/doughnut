package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A proposal published against the pre-Trash accepted head is rejected by the existing stale-head
 * rule, leaving the complete Trash result intact.
 */
class NotebookGitWebTrashStaleProposalControllerTest
    extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String EDITED_BODY = "---\ntype: Note\n---\nlocal edit on pre-trash head";

  @Test
  void proposalOnStalePreTrashHeadIsRejectedAfterTrash() throws Exception {
    LiveCellsFixture f = seedLiveCellsInBiology();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    noteController.trashNote(f.cells(), leaveDeadLinks());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());

    byte[] staleProposal =
        proposalBundleBytes(
            binding(f.notebook()),
            List.of(new NotebookGitProposalFile("Biology/Cells.md", EDITED_BODY)));
    ResponseStatusException rejection =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    f.notebook().getId(), acceptedA.getName(), staleProposal));

    assertThat(rejection.getStatusCode(), is(HttpStatus.CONFLICT));
    CommittedTrashState committed =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note cells = noteRepository.findById(f.cells().getId()).orElseThrow();
              NotebookGitBinding binding = binding(f.notebook());
              return new CommittedTrashState(
                  cells.isTrashed(),
                  cells.getFolder().getParentFolder().getName(),
                  ObjectId.fromString(binding.getAcceptedGitObjectId()));
            });
    assertThat(committed.trashed(), is(true));
    assertThat(committed.trashParentName(), equalTo("_trash"));
    assertThat(committed.acceptedHead(), equalTo(acceptedB));
    byte[] downloaded = controller.downloadNotebookGitBundle(f.notebook()).getBody();
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repo, downloaded);
      assertThat(head, equalTo(acceptedB));
      assertThat(
          GitBundleTestReader.pathsIn(repo, head),
          containsInAnyOrder("Biology/.keep", "_trash/Biology/Cells.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, head, "_trash/Biology/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(NotebookGitProposalBlobText.readUtf8(repo, head, "Biology/.keep"), equalTo(""));
    }
  }

  LiveCellsFixture seedLiveCellsInBiology() throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new LiveCellsFixture(notebook, cells);
  }

  record LiveCellsFixture(Notebook notebook, Note cells) {}

  record CommittedTrashState(boolean trashed, String trashParentName, ObjectId acceptedHead) {}
}
