package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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

/**
 * Rejects proposals that remove a reserved folder README or a non-regular file from the accepted
 * tree. These refusals are about the accepted tree's reserved/shape contract, not uncertain
 * identity, so they live apart from the identity-uncertain deletion rejection cases.
 */
class NotebookGitReservedFileRejectionControllerTest extends NotebookGitControllerTestBase {

  private static final String ORIGINAL = "---\ntype: Note\n---\noriginal content";

  @ParameterizedTest
  @MethodSource("invalidRemovedFiles")
  void rejectsRemovalOfAReservedOrNonRegularFile(String path, FileMode mode, String reason)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] acceptedBundle =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile(path, ORIGINAL, mode)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      binding =
          seedAcceptedHistory(
              notebook, repository, GitBundleTestReader.fetchHead(repository, acceptedBundle));
    }

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
        Arguments.of("note.md", FileMode.EXECUTABLE_FILE, "not a regular file mode"),
        Arguments.of("note.md", FileMode.SYMLINK, "not a regular file mode"));
  }
}
