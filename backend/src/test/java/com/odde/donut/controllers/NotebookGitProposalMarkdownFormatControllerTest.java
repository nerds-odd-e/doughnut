package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
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
  void rejectsDuplicateKeysInAnEditWithoutChangingTheNoteOrAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String originalContent = "---\ntype: Note\n---\nOriginal body.\n";
    Note note =
        makeMe.aNote().notebook(notebook).title("Existing").content(originalContent).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "Existing.md",
                    "---\ntype: Note\nauthor: first\nauthor: second\n---\nChanged body.\n")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Existing.md"));
    assertThat(exception.getReason(), containsString("duplicate"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), equalTo(originalContent));
  }

  @Test
  void rejectsNoteWithNoFrontmatterFenceWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("note.md", "changed content, no frontmatter at all"),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

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
                new NotebookGitProposalFile(
                    "note.md", "---\ntype: [unclosed, \"bracket\n---\nchanged content"),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

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
                new NotebookGitProposalFile("note.md", "---\n- a\n- b\n---\nchanged content"),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

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
                new NotebookGitProposalFile(
                    "note.md", "---\ncustom_field: hello\n---\nchanged content"),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

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
                new NotebookGitProposalFile("note.md", "---\ntype:\n---\nchanged content"),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

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
                new NotebookGitProposalFile("note.md", invalidUtf8),
                new NotebookGitProposalFile(
                    "README.md", "---\ntype: Readme\n---\nreadme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }
}
