package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal}'s typed-Markdown gating: once a proposal clears the
 * tree-shape gate, every {@code .md} blob in the proposed tree must be strictly valid UTF-8 with a
 * leading {@code ---} fenced YAML block that parses to a mapping carrying a non-blank {@code type}.
 * An author-chosen {@code type} value outside Donut's recognized canonical types is still valid
 * here.
 */
class NotebookGitProposalMarkdownFormatControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void ownerSubmittingAnUnknownTypeAndUnrecognizedKeyStillReceivesTheInterimRefusal()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile(
                    "note.md",
                    "---\ntype: SomethingCustom\ncustom_field: hello\n---\nchanged content"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void rejectsNoteWithNoFrontmatterFenceWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "changed content, no frontmatter at all"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithMalformedFrontmatterYamlWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile(
                    "note.md", "---\ntype: [unclosed, \"bracket\n---\nchanged content"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithNonMappingFrontmatterWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "---\n- a\n- b\n---\nchanged content"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithMissingTypeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "---\ncustom_field: hello\n---\nchanged content"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithBlankTypeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "---\ntype:\n---\nchanged content"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithInvalidUtf8BytesWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] invalidUtf8 = {(byte) 0x80, 'x'};
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", invalidUtf8),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }
}
