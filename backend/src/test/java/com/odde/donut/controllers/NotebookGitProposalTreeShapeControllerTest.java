package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.odde.donut.entities.Folder;
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
  void acceptsAChangeToIndexMdJustLikeAnyOtherNote() throws Exception {
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
            List.of(
                new NotebookGitProposalFile("index.md", "---\ntype: Note\n---\nchanged content")));

    assertDoesNotThrow(
        () ->
            controller.publishNotebookGitProposal(
                notebook.getId(), binding.getAcceptedGitObjectId(), bundleBytes));
  }

  @Test
  void rejectsProposalWhoseParentFolderIsMissingFromAcceptedPortableContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "Folder/extra.md", "---\ntype: Note\n---\nextra content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Folder/extra.md"));
    assertThat(
        exception.getReason(), containsString("not represented in accepted Portable content"));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
  }

  @Test
  void rejectsProposalThatDeletesAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("README.md", "readme original")));

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
                new NotebookGitProposalFile("renamed.md", "original content"),
                new NotebookGitProposalFile("README.md", "readme original")));

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
                new NotebookGitProposalFile("note.md", "original content"),
                new NotebookGitProposalFile("README.md", "readme changed")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("README.md"));
  }

  @Test
  void identifiesAReservedReadmeAmongNoteChangesWithoutMutatingRemoteState() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Folder")
            .readmeContent("original readme")
            .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Added.md", "---\ntype: Note\n---\nadded content"),
                new NotebookGitProposalFile(
                    "Folder/README.md", "---\ntype: Readme\n---\nchanged readme")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Folder/README.md"));
    assertThat(exception.getReason(), containsString("folder README, which is reserved"));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
    assertThat(
        entityManager.find(Folder.class, folder.getId()).getReadmeContent(),
        equalTo("original readme"));
  }

  @Test
  void rejectsProposalThatChangesAFilesModeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("note.md", "changed content", FileMode.EXECUTABLE_FILE),
                new NotebookGitProposalFile("README.md", "readme original")));

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
