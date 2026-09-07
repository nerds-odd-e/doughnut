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
 * identical-heads no-op must change one regular Markdown note, include an addition among several
 * added or modified notes, delete exactly one ordinary note in isolation, or rename exactly one
 * ordinary note within the same parent folder with unchanged content. Same-parent rename acceptance
 * is covered separately in {@link NotebookGitProposalRenameControllerTest}.
 */
class NotebookGitProposalTreeShapeControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Test
  void rejectsSeveralExistingNoteEditsWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("First").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Second").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String changed = "---\ntype: Note\n---\nchanged content";
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("First.md", changed),
                new NotebookGitProposalFile("Second.md", changed)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("multiple changed files"));
    assertThat(
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .map(note -> note.getContent())
            .toList(),
        equalTo(List.of(TYPED_NOTE_CONTENT, TYPED_NOTE_CONTENT)));
  }

  @Test
  void acceptsAChangeToIndexMdJustLikeAnyOtherNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("index").content(TYPED_NOTE_CONTENT).please();
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
  void rejectsProposalThatMovesAFileAcrossParentFoldersWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Folder/note.md", "original content"),
                new NotebookGitProposalFile("README.md", "readme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("isolated deletion"));
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
