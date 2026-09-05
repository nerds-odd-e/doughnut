package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.util.List;
import org.eclipse.jgit.lib.FileMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal}'s tree-shape gating: a proposal that is not an
 * identical-heads no-op must change exactly one regular Markdown note, and nothing else.
 */
class NotebookGitProposalTreeShapeControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void ownerSubmittingAValidChildProposalStillReceivesTheInterimRefusal() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe
        .aNote()
        .notebook(notebook)
        .title("note")
        .content("---\ntype: Note\n---\noriginal content")
        .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding, List.of(new ProposedFile("note.md", "---\ntype: Note\n---\nchanged content")));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void ownerChangingIndexMdReachesTheInterimRefusalJustLikeAnyOtherNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe
        .aNote()
        .notebook(notebook)
        .title("index")
        .content("---\ntype: Note\n---\noriginal content")
        .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(new ProposedFile("index.md", "---\ntype: Note\n---\nchanged content")));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void rejectsProposalThatAddsAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "original content"),
                new ProposedFile("README.md", "readme original"),
                new ProposedFile("extra.md", "extra content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("extra.md"));
  }

  @Test
  void rejectsProposalThatDeletesAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(binding, List.of(new ProposedFile("README.md", "readme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsProposalThatMovesAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("renamed.md", "original content"),
                new ProposedFile("README.md", "readme original")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsProposalThatChangesTheReservedReadmeWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "original content"),
                new ProposedFile("README.md", "readme changed")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("README.md"));
  }

  @Test
  void rejectsProposalThatChangesTwoFilesWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "changed content"),
                new ProposedFile("README.md", "readme changed")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsProposalThatChangesAFilesModeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "changed content", FileMode.EXECUTABLE_FILE),
                new ProposedFile("README.md", "readme original")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }

  /** The baseline accepted tree shared by the tree-shape rejection tests: one note, one README. */
  private static List<PortableTreeEntry> baselineEntries() {
    return List.of(
        new PortableTreeEntry("note.md", "original content"),
        new PortableTreeEntry("README.md", "readme original"));
  }
}
