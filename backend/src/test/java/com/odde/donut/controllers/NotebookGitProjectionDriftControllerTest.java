package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies that a Git proposal cannot overwrite accepted content or unsupported structural drift.
 */
class NotebookGitProjectionDriftControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nproposed content";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb content";

  @Autowired TextContentController textContentController;
  @Autowired RelationController relationController;

  @Test
  void rejectsAnAdditionBasedOnAnOldParentAfterWebContentAdvancedAcceptedMain() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("note.md", ACCEPTED_CONTENT),
                new NotebookGitProposalFile("addition.md", PROPOSED_CONTENT)));
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent(WEB_CONTENT);
    textContentController.updateNoteContent(note, update);
    NotebookGitBinding winningBinding = reloadCommittedBinding(notebook.getId());
    assertThat(
        winningBinding.getAcceptedGitObjectId(), not(equalTo(binding.getAcceptedGitObjectId())));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("expectedHead no longer matches"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(update.getContent()));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
        equalTo(winningBinding.getAcceptedGitObjectId()));
  }

  @Test
  void rejectsWhenWebStructureHasDriftedFromAcceptedMain() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("folder").please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    relationController.moveNoteToFolder(note, folder);

    ResponseStatusException exception = submitCurrentParentProposal(notebook, binding);

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(exception.getReason(), containsString("refresh the checkout before publishing"));
  }

  private ResponseStatusException submitCurrentParentProposal(
      Notebook notebook, NotebookGitBinding binding) throws Exception {
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("note.md", PROPOSED_CONTENT)));
    return assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);
  }

  private NotebookGitBinding reloadCommittedBinding(Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
  }
}
