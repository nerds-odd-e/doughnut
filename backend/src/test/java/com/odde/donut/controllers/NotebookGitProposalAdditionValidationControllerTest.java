package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Publish-boundary examples for diagnosing unsupported local note additions. */
class NotebookGitProposalAdditionValidationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String VALID_CONTENT = "---\ntype: Note\n---\nAuthored content.\n";

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
