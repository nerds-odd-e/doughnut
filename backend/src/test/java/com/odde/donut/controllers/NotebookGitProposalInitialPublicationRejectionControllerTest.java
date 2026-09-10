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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitProposalInitialPublicationRejectionControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved notebook readme.\n";
  private static final String SECOND_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved second note.\n";

  @Autowired FolderRepository folderRepository;

  @Test
  void rejectsInvalidNestedRelationshipContentWithoutPartialPublication() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    PublicationFootprint before = committedFootprint(notebook);
    assertThat(before.folders(), empty());
    assertThat(before.notes(), empty());
    assertThat(before.references(), empty());
    String relationshipPath = "Topic/Nested/Z-related-to-B.md";
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("Topic/README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("Sibling/B.md", SECOND_NOTE),
                new NotebookGitProposalFile("A.md", "---\ntype: Note\n---\nSee [[Sibling/B]].\n"),
                new NotebookGitProposalFile(
                    relationshipPath,
                    "---\ntype: Relationship\nnote_level: 7\n---\ninvalid content")));

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
    assertThat(committedFootprint(notebook), equalTo(before));
  }

  @Test
  void rejectsInvalidFolderAncestorWithoutPartialPublication() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    PublicationFootprint before = committedFootprint(notebook);
    String invalidPath = "Topic/ /Nested/B.md";
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("Earlier/README.md", NOTEBOOK_README),
                new NotebookGitProposalFile(
                    "Earlier/A.md", "---\ntype: Note\n---\nSee [[Topic/B]].\n"),
                new NotebookGitProposalFile(invalidPath, SECOND_NOTE)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(
        exception.getReason(),
        equalTo("Invalid folder name at path \"" + invalidPath + "\": must not be blank"));
    assertThat(committedFootprint(notebook), equalTo(before));
  }

  private PublicationFootprint committedFootprint(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            new PublicationFootprint(
                notebookRepository.findById(notebook.getId()).orElseThrow().getReadmeContent(),
                folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()),
                noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()),
                rowsForNotebook(entityManager, notebook.getId())));
  }

  private record PublicationFootprint(
      String readme,
      List<Folder> folders,
      List<Note> notes,
      List<AuthoredNoteReferenceRow> references) {}
}
