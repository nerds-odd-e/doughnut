package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.testability.GitBundleTestReader;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies {@code publishNotebookGitProposal}'s tree-shape gating: a proposal that is not an
 * identical-heads no-op must change exactly one regular Markdown note, and nothing else.
 */
class NotebookGitProposalTreeShapeControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void ownerSubmittingAValidChildProposalStillReceivesTheInterimRefusal() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "changed content"),
                new ProposedFile("README.md", "readme original")));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void ownerChangingIndexMdReachesTheInterimRefusalJustLikeAnyOtherNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        seedAcceptedBinding(
            notebook,
            List.of(
                new PortableTreeEntry("index.md", "original content"),
                new PortableTreeEntry("README.md", "readme original")));
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("index.md", "changed content"),
                new ProposedFile("README.md", "readme original")));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void rejectsProposalThatAddsAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "original content"),
                new ProposedFile("README.md", "readme original"),
                new ProposedFile("extra.md", "extra content")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("extra.md"));
  }

  @Test
  void rejectsProposalThatDeletesAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(binding, List.of(new ProposedFile("README.md", "readme original")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("note.md"));
  }

  @Test
  void rejectsProposalThatMovesAFileWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("renamed.md", "original content"),
                new ProposedFile("README.md", "readme original")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsProposalThatChangesTheReservedReadmeWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "original content"),
                new ProposedFile("README.md", "readme changed")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("README.md"));
  }

  @Test
  void rejectsProposalThatChangesTwoFilesWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "changed content"),
                new ProposedFile("README.md", "readme changed")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsProposalThatChangesAFilesModeWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, baselineEntries());
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "changed content", FileMode.EXECUTABLE_FILE),
                new ProposedFile("README.md", "readme original")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), bundleBytes, HttpStatus.BAD_REQUEST);
  }

  /** The baseline accepted tree shared by the tree-shape rejection tests: one note, one README. */
  private static List<PortableTreeEntry> baselineEntries() {
    return List.of(
        new PortableTreeEntry("note.md", "original content"),
        new PortableTreeEntry("README.md", "readme original"));
  }

  /**
   * Testability-only: overwrites {@code notebook}'s accepted Git binding with a fresh root commit
   * built directly from {@code entries}, so tree-shape tests can control the accepted tree's exact
   * shape without depending on the notebook's own note/folder content.
   */
  private NotebookGitBinding seedAcceptedBinding(
      Notebook notebook, List<PortableTreeEntry> entries) {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "System", "system@example.com", "Seed content", Instant.now())) {
      NotebookGitBundleWriter.BundleWriteResult written = NotebookGitBundleWriter.write(repository);
      binding.setAcceptedGitObjectId(written.headObjectId());
      binding.setBundleBytes(written.bundleBytes());
    }
    return notebookGitBindingRepository.save(binding);
  }

  /** A bundle whose {@code main} is a single-parent child of {@code binding}'s accepted head. */
  private byte[] proposalBundleBytes(NotebookGitBinding binding, List<ProposedFile> proposedFiles)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId childCommit =
          commitOnTopOf(repository, List.of(acceptedHead), proposedFiles, "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }
}
