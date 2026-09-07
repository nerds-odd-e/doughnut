package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.FileMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitDeletionRejectionControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL = "---\ntype: Note\n---\noriginal content";

  @ParameterizedTest
  @MethodSource("mixedDeletionProposals")
  void requiresAnIsolatedDeletionCommitWithoutChangingAcceptedNotes(
      List<NotebookGitProposalFile> proposedFiles) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    List<Integer> originalIds =
        inCommittedTransaction(
            transactionManager,
            () ->
                List.of(
                    makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL).please().getId(),
                    makeMe.aNote().notebook(notebook).title("Second").content(ORIGINAL).please().getId()));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(binding, proposedFiles),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("isolated deletion commit"));
    inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(liveNotes.stream().map(Note::getId).toList(), equalTo(originalIds));
          assertThat(liveNotes.stream().map(Note::getContent).toList(), equalTo(List.of(ORIGINAL, ORIGINAL)));
        });
  }

  static Stream<Arguments> mixedDeletionProposals() {
    NotebookGitProposalFile second = new NotebookGitProposalFile("Second.md", ORIGINAL);
    return Stream.of(
        Arguments.of(List.of(new NotebookGitProposalFile("Second.md", "---\ntype: Note\n---\nedited"))),
        Arguments.of(List.of(second, new NotebookGitProposalFile("Added.md", "---\ntype: Note\n---\nnew"))),
        Arguments.of(List.of(second, new NotebookGitProposalFile("Renamed.md", ORIGINAL))),
        Arguments.of(List.of()));
  }

  @ParameterizedTest
  @MethodSource("invalidRemovedFiles")
  void rejectsRemovalOfAReservedOrNonRegularFile(
      String path, FileMode mode, String reason) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] acceptedBundle =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile(path, ORIGINAL, mode)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      binding.setAcceptedGitObjectId(GitBundleTestReader.fetchHead(repository, acceptedBundle).name());
    }
    binding.setBundleBytes(acceptedBundle);
    notebookGitBindingRepository.save(binding);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(binding, List.of()),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString(path));
    assertThat(exception.getReason(), containsString(reason));
  }

  static Stream<Arguments> invalidRemovedFiles() {
    return Stream.of(
        Arguments.of("README.md", FileMode.REGULAR_FILE, "folder README, which is reserved"),
        Arguments.of("Folder/README.md", FileMode.REGULAR_FILE, "folder README, which is reserved"),
        Arguments.of("note.txt", FileMode.REGULAR_FILE, "not a Markdown note"),
        Arguments.of("note.md", FileMode.EXECUTABLE_FILE, "not a regular file mode"),
        Arguments.of("note.md", FileMode.SYMLINK, "not a regular file mode"));
  }
}
