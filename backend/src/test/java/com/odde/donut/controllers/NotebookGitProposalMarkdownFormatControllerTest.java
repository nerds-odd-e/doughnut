package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal}'s typed-Markdown gating: every {@code .md} blob the
 * proposal adds or changes must be strictly valid UTF-8 with a leading {@code ---} fenced YAML
 * block that parses to a mapping carrying a non-blank {@code type}, whether or not the proposal
 * relocates a folder. Markdown already in accepted history is not judged again. An author-chosen
 * {@code type} value outside Donut's recognized canonical types is still valid here.
 */
class NotebookGitProposalMarkdownFormatControllerTest extends NotebookGitControllerTestBase {

  private static final String TYPED_NOTE = "---\ntype: Note\n---\nbody";

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
    NotebookGitBinding binding = snapshotWithTypedNote(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("note.md", "changed content, no frontmatter at all")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithMalformedFrontmatterYamlWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotWithTypedNote(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "note.md", "---\ntype: [unclosed, \"bracket\n---\nchanged content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithNonMappingFrontmatterWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotWithTypedNote(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(new NotebookGitProposalFile("note.md", "---\n- a\n- b\n---\nchanged content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithMissingTypeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotWithTypedNote(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "note.md", "---\ncustom_field: hello\n---\nchanged content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithBlankTypeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotWithTypedNote(notebook);
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(new NotebookGitProposalFile("note.md", "---\ntype:\n---\nchanged content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsNoteWithInvalidUtf8BytesWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotWithTypedNote(notebook);
    byte[] invalidUtf8 = {(byte) 0x80, 'x'};
    byte[] bundleBytes =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile("note.md", invalidUtf8)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsAnUntypedNoteAddedUnderARelocatedFolderWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(source).title("A").content(TYPED_NOTE).please();
    makeMe.aNote().folder(destination).title("keep").content(TYPED_NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Dest/keep.md", TYPED_NOTE),
                new NotebookGitProposalFile("Dest/Source/A.md", TYPED_NOTE),
                new NotebookGitProposalFile("Dest/Source/Untyped.md", "no frontmatter at all")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Invalid Markdown"));
    assertThat(exception.getReason(), containsString("Dest/Source/Untyped.md"));
  }

  @Test
  void publishesAnEditBesideAnUntouchedAcceptedNoteWithoutFrontmatter() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String legacyContent = "legacy body without frontmatter";
    makeMe.aNote().notebook(notebook).title("Legacy").content(legacyContent).please();
    Note edited = makeMe.aNote().notebook(notebook).title("Notes").content(TYPED_NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String changedContent = "---\ntype: Note\n---\nChanged body.\n";
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Legacy.md", legacyContent),
                new NotebookGitProposalFile("Notes.md", changedContent)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    assertThat(
        noteRepository.findById(edited.getId()).orElseThrow().getContent(),
        equalTo(changedContent));
  }

  private NotebookGitBinding snapshotWithTypedNote(Notebook notebook) {
    makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE).please();
    return snapshotCurrentPortableTree(notebook);
  }
}
