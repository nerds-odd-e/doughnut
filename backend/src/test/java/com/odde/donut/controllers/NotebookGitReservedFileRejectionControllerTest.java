package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.donut.entities.Folder;
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

/**
 * Rejects proposals that remove a reserved folder README or a non-regular file from the accepted
 * tree. These refusals are about the accepted tree's reserved/shape contract, not uncertain
 * identity, so they live apart from the identity-uncertain deletion rejection cases.
 */
class NotebookGitReservedFileRejectionControllerTest extends NotebookGitControllerTestBase {

  private static final String ORIGINAL = "---\ntype: Note\n---\noriginal content";

  @ParameterizedTest
  @MethodSource("nonRegularFiles")
  void rejectsRemovalOfANonRegularFile(String path, FileMode mode, String reason) throws Exception {
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

  @Test
  void rejectsRemovalOfTheNotebookReadme() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.theNotebook(notebook).readmeContent(README_BODY).please();

    assertReadmeRemovalRejected(notebook, List.of(), "README.md");
  }

  @Test
  void rejectsRemovalOfAFolderReadme() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder =
        makeMe.aFolder().notebook(notebook).name("Folder").readmeContent(README_BODY).please();
    makeMe.aNote().folder(folder).title("A").content(NOTE).please();

    assertReadmeRemovalRejected(
        notebook, List.of(new NotebookGitProposalFile("Folder/A.md", NOTE)), "Folder/README.md");
  }

  private void assertReadmeRemovalRejected(
      Notebook notebook, List<NotebookGitProposalFile> proposed, String readmePath)
      throws Exception {
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(binding, proposed),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString(readmePath));
    assertThat(exception.getReason(), containsString("folder README, which is reserved"));
  }

  static Stream<Arguments> nonRegularFiles() {
    return Stream.of(
        Arguments.of("note.md", FileMode.EXECUTABLE_FILE, "not a regular file mode"),
        Arguments.of("note.md", FileMode.SYMLINK, "not a regular file mode"));
  }
}
