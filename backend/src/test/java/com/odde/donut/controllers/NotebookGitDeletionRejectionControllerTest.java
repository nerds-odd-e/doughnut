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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitDeletionRejectionControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL = "---\ntype: Note\n---\noriginal content";
  private static final String OTHER = "---\ntype: Note\n---\nother content";
  private static final String EDITED = "---\ntype: Note\n---\nedited content";
  private static final String NEW = "---\ntype: Note\n---\nnew";
  private static final String INVALID_EDIT = "---\ncustom: value\n---\nChanged body.\n";

  @ParameterizedTest
  @MethodSource("identityUncertainDeletionProposals")
  void refusesMixingRemovalsWithAdditionsWithoutChangingAcceptedNotes(
      List<NotebookGitProposalFile> proposedFiles) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    List<Integer> originalIds =
        inCommittedTransaction(
            transactionManager,
            () ->
                List.of(
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("First")
                        .content(ORIGINAL)
                        .please()
                        .getId(),
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("Second")
                        .content(ORIGINAL)
                        .please()
                        .getId(),
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("Third")
                        .content(OTHER)
                        .please()
                        .getId()));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(binding, proposedFiles),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("identity correspondence is uncertain"));
    inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> liveNotes =
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(liveNotes.stream().map(Note::getId).toList(), equalTo(originalIds));
          assertThat(
              liveNotes.stream().map(Note::getContent).toList(),
              equalTo(List.of(ORIGINAL, ORIGINAL, OTHER)));
        });
  }

  static Stream<Arguments> identityUncertainDeletionProposals() {
    NotebookGitProposalFile second = new NotebookGitProposalFile("Second.md", ORIGINAL);
    NotebookGitProposalFile third = new NotebookGitProposalFile("Third.md", OTHER);
    return Stream.of(
        // unequal-blob removal + addition
        Arguments.of(List.of(second, third, new NotebookGitProposalFile("Added.md", NEW))),
        // one source and two equal-blob destinations
        Arguments.of(
            List.of(
                second,
                third,
                new NotebookGitProposalFile("MovedA.md", ORIGINAL),
                new NotebookGitProposalFile("MovedB.md", ORIGINAL))),
        // two sources and one equal-blob destination
        Arguments.of(List.of(third, new NotebookGitProposalFile("Moved.md", ORIGINAL))),
        // two sources and two equal-blob destinations
        Arguments.of(
            List.of(
                third,
                new NotebookGitProposalFile("MovedA.md", ORIGINAL),
                new NotebookGitProposalFile("MovedB.md", ORIGINAL))),
        // an ambiguity alongside a separate unique pair
        Arguments.of(
            List.of(
                new NotebookGitProposalFile("MovedA.md", ORIGINAL),
                new NotebookGitProposalFile("MovedB.md", ORIGINAL),
                new NotebookGitProposalFile("Unique.md", OTHER))),
        // a unique pair plus residual unequal removal and addition
        Arguments.of(
            List.of(
                second,
                new NotebookGitProposalFile("Unique.md", OTHER),
                new NotebookGitProposalFile("Added.md", NEW))));
  }

  @Test
  void refusesAMoveWhenACompanionEditIsInvalidWithoutChangingAcceptedNotes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    List<Integer> originalIds =
        inCommittedTransaction(
            transactionManager,
            () ->
                List.of(
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("First")
                        .content(ORIGINAL)
                        .please()
                        .getId(),
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("Second")
                        .content(ORIGINAL)
                        .please()
                        .getId()));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Renamed.md", ORIGINAL),
                new NotebookGitProposalFile("Second.md", INVALID_EDIT))),
        HttpStatus.BAD_REQUEST);

    inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> liveNotes =
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(liveNotes.stream().map(Note::getId).toList(), equalTo(originalIds));
          assertThat(
              liveNotes.stream().map(Note::getContent).toList(),
              equalTo(List.of(ORIGINAL, ORIGINAL)));
          assertThat(
              liveNotes.stream().map(Note::getTitle).toList(), equalTo(List.of("First", "Second")));
        });
  }

  @ParameterizedTest
  @MethodSource("invalidRemovedFiles")
  void rejectsRemovalOfAReservedOrNonRegularFile(String path, FileMode mode, String reason)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] acceptedBundle =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile(path, ORIGINAL, mode)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      binding.setAcceptedGitObjectId(
          GitBundleTestReader.fetchHead(repository, acceptedBundle).name());
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
