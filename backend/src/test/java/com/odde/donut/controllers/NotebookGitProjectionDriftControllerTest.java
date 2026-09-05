package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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

/** Verifies that a Git proposal cannot overwrite Portable content changed through the web. */
class NotebookGitProjectionDriftControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nproposed content";

  @Autowired TextContentController textContentController;
  @Autowired RelationController relationController;

  @Test
  void rejectsWhenWebContentHasDriftedFromAcceptedMain() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent("---\ntype: Note\n---\nweb content");
    textContentController.updateNoteContent(note, update);

    ResponseStatusException exception = submitCurrentParentProposal(notebook, binding);

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(exception.getReason(), containsString("web changes cannot yet be synchronized"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(update.getContent()));
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
    assertThat(exception.getReason(), containsString("web changes cannot yet be synchronized"));
  }

  private ResponseStatusException submitCurrentParentProposal(
      Notebook notebook, NotebookGitBinding binding) throws Exception {
    byte[] proposal =
        proposalBundleBytes(binding, List.of(new ProposedFile("note.md", PROPOSED_CONTENT)));
    return assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.CONFLICT);
  }
}
