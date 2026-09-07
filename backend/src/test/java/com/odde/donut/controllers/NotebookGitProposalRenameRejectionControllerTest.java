package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal} rejects same-parent rename-shaped proposals whose new
 * filename or raw tree entries violate the existing Portable destination contract: an invalid or
 * normalizable title, the reserved {@code README.md} name, a non-regular file mode on either side,
 * or a destination path already occupied by a different live note's content. Clean rename
 * acceptance is covered separately in {@link NotebookGitProposalRenameControllerTest}.
 */
class NotebookGitProposalRenameRejectionControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Test
  void rejectsARenameWhoseNewFilenameYieldsAnInvalidTitle() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("bad:name.md", TYPED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("bad:name.md"));
    assertThat(exception.getReason(), containsString("not contain"));
    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getTitle(), equalTo("note"));
    assertThat(reloaded.getContent(), equalTo(TYPED_NOTE_CONTENT));
  }

  @Test
  void rejectsARenameIntoTheReservedReadmeName() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("README.md", TYPED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("README.md"));
    assertThat(exception.getReason(), containsString("reserved"));
    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getTitle(), equalTo("note"));
  }

  @Test
  void rejectsARenameShapedPairWhoseOldSideHasANonRegularMode() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        seedAcceptedBindingWithFileMode(
            notebook, "note.md", "original content", FileMode.EXECUTABLE_FILE);
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("renamed.md", "original content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
    assertThat(exception.getReason(), containsString("not a regular file mode"));
  }

  @Test
  void rejectsARenameShapedPairWhoseNewSideHasANonRegularMode() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        seedAcceptedBinding(
            notebook, List.of(new PortableTreeEntry("note.md", "original content")));
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "renamed.md", "original content", FileMode.EXECUTABLE_FILE)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("renamed.md"));
    assertThat(exception.getReason(), containsString("not a regular file mode"));
  }

  @Test
  void rejectsADeletionMixedWithAModifiedNoteOccupyingTheWouldBeDestination() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String occupiedContent = "---\ntype: Note\n---\noccupied content";
    Note source =
        makeMe.aNote().notebook(notebook).title("Source").content(TYPED_NOTE_CONTENT).please();
    Note occupied =
        makeMe.aNote().notebook(notebook).title("Occupied").content(occupiedContent).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String changedOccupiedContent = "---\ntype: Note\n---\nchanged occupied content";
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Occupied.md", changedOccupiedContent)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("isolated deletion"));
    Note reloadedSource = noteRepository.findById(source.getId()).orElseThrow();
    assertThat(reloadedSource.getContent(), equalTo(TYPED_NOTE_CONTENT));
    Note reloadedOccupied = noteRepository.findById(occupied.getId()).orElseThrow();
    assertThat(reloadedOccupied.getContent(), equalTo(occupiedContent));
  }

  /**
   * Testability-only: like {@link #seedAcceptedBinding}, but writes one raw entry at the given
   * {@link FileMode} instead of always {@code REGULAR_FILE}, so non-regular-mode rejection can be
   * exercised on the accepted (old) side of a rename-shaped pair.
   */
  private NotebookGitBinding seedAcceptedBindingWithFileMode(
      Notebook notebook, String path, String content, FileMode mode) throws Exception {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId commitId =
          commitOnTopOf(
              repository,
              List.of(),
              List.of(new NotebookGitProposalFile(path, content, mode)),
              "Seed content");
      binding.setAcceptedGitObjectId(commitId.getName());
      binding.setBundleBytes(bundleBytesForHead(repository, commitId));
    }
    return notebookGitBindingRepository.save(binding);
  }
}
