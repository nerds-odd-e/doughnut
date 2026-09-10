package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsForNotebook;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that an invalid authored property on the root Relationship in the initial mixed
 * composition rejects without partial publication. Successful publication and wiki-link navigation
 * of that layout are covered in {@link NotebookGitProposalInitialRootRelationshipControllerTest}.
 */
class NotebookGitProposalInitialRootRelationshipRejectionControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved notebook readme.\n";
  private static final String SECOND_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved second note.\n";

  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsInvalidRelationshipContentWithoutPartialPublication() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String readmeBefore =
        notebookRepository.findById(notebook.getId()).orElseThrow().getReadmeContent();
    List<Folder> foldersBefore =
        inCommittedTransaction(
            transactionManager,
            () -> folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()));
    List<Note> notesBefore =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()));
    List<AuthoredNoteReferenceRow> referencesBefore =
        inCommittedTransaction(
            transactionManager, () -> rowsForNotebook(entityManager, notebook.getId()));
    assertThat(foldersBefore, empty());
    assertThat(notesBefore, empty());
    assertThat(referencesBefore, empty());
    String relationshipPath = "A-related-to-B.md";
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    relationshipPath,
                    "---\ntype: Relationship\nnote_level: 7\n---\ninvalid content"),
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("B.md", SECOND_NOTE),
                new NotebookGitProposalFile(
                    "A.md", "---\ntype: Note\n---\nSee [[rollback target]].\n")));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, ApiException.class);

    assertThat(exception.getErrorBody().getMessage(), containsString(relationshipPath));
    assertThat(
        exception.getErrorBody().getMessage(),
        containsString(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    assertThat(
        exception.getErrorBody().getErrors().get("note_level"),
        equalTo(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    assertThat(
        notebookRepository.findById(notebook.getId()).orElseThrow().getReadmeContent(),
        equalTo(readmeBefore));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()),
              equalTo(foldersBefore));
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()),
              equalTo(notesBefore));
          assertThat(rowsForNotebook(entityManager, notebook.getId()), equalTo(referencesBefore));
        });
  }
}
