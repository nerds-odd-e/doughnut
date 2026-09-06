package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Publish-boundary examples for diagnosing unsupported local note additions. */
class NotebookGitProposalAdditionValidationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String VALID_CONTENT = "---\ntype: Note\n---\nAuthored content.\n";

  @Test
  void rejectsTheWholeMixedProposalWhenALaterAddedNoteHasNoType() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(VALID_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Added.md", VALID_CONTENT),
                new NotebookGitProposalFile(
                    "Existing.md", "---\ntype: Note\n---\nChanged content.\n"),
                new NotebookGitProposalFile(
                    "Missing type.md", "---\ncustom: value\n---\nBody.\n")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Missing type.md"));
    assertThat(
        exception.getReason(), containsString("frontmatter is missing a non-blank \"type\""));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
          assertThat(
              noteRepository.findById(existing.getId()).orElseThrow().getContent(),
              equalTo(VALID_CONTENT));
        });
  }

  @ParameterizedTest
  @MethodSource("invalidAdditions")
  void explainsWhyAnAddedRootNoteIsInvalid(String path, String content, String expectedReason)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile(path, content)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString(path));
    assertThat(exception.getReason(), containsString(expectedReason));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
  }

  private static Stream<Arguments> invalidAdditions() {
    return Stream.of(
        Arguments.of("Missing type.md", "---\ncustom: value\n---\nBody.\n", "type"),
        Arguments.of("Invalid type.md", "---\ntype: [Note]\n---\nBody.\n", "type"),
        Arguments.of("Invalid YAML.md", "---\ntype: [broken\n---\nBody.\n", "malformed"),
        Arguments.of(
            "Duplicate Keys.md",
            "---\ntype: Note\nauthor: first\nauthor: second\n---\nBody.\n",
            "duplicate"),
        Arguments.of(
            "Nested Duplicate Keys.md",
            "---\ntype: Note\ncustom:\n  source: first\n  source: second\n---\nBody.\n",
            "duplicate"),
        Arguments.of("bad:name.md", VALID_CONTENT, "not contain"),
        Arguments.of("readme.md", VALID_CONTENT, "reserved"),
        Arguments.of(" Trimmed .md", VALID_CONTENT, "normalized"));
  }
}
