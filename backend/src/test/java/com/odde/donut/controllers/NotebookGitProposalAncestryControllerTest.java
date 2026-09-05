package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Verifies that notebook Git proposals match the accepted head and its required ancestry. */
class NotebookGitProposalAncestryControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void ownerSubmittingAProposalIdenticalToAcceptedHeadStillReceivesTheInterimRefusal()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), binding.getBundleBytes()));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void rejectsStaleExpectedHeadWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        "0000000000000000000000000000000000000000",
        singleParentChildBundleBytes(binding),
        HttpStatus.CONFLICT);
  }

  @Test
  void rejectsProposalWithNoParentOrUnrelatedHistoryWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        validProposalBundleBytes(),
        HttpStatus.CONFLICT);
  }

  @Test
  void rejectsMergeCommitProposalWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        mergeCommitBundleBytes(binding),
        HttpStatus.CONFLICT);
  }

  @Test
  void rejectsProposalSeveralCommitsAheadOfAcceptedHeadWithoutMutatingTheAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        multipleCommitsAheadBundleBytes(binding),
        HttpStatus.CONFLICT);
  }

  /** A well-formed, parentless root commit bundle unrelated to any notebook's accepted history. */
  private byte[] validProposalBundleBytes() {
    List<PortableTreeEntry> entries = List.of(new PortableTreeEntry("README.md", "proposal"));
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "Proposer", "proposer@example.com", "Proposal", Instant.now())) {
      return NotebookGitBundleWriter.write(repository).bundleBytes();
    }
  }

  /** A bundle whose {@code main} is a genuine single-parent child of the accepted head. */
  private byte[] singleParentChildBundleBytes(NotebookGitBinding binding) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId childCommit =
          commitOnTopOf(
              repository, List.of(acceptedHead), "proposal.md", "proposal content", "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }

  /**
   * A bundle whose {@code main} is a merge commit with two parents, one of which is the accepted
   * head - still rejected, because ancestry here requires exactly one parent.
   */
  private byte[] mergeCommitBundleBytes(NotebookGitBinding binding) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId otherParent =
          commitOnTopOf(repository, List.of(), "other.md", "other content", "Other root commit");
      ObjectId mergeCommit =
          commitOnTopOf(
              repository,
              List.of(acceptedHead, otherParent),
              "merged.md",
              "merged content",
              "Merge commit");
      return bundleBytesForHead(repository, mergeCommit);
    }
  }

  /** A bundle whose {@code main} is two single-parent commits ahead of the accepted head. */
  private byte[] multipleCommitsAheadBundleBytes(NotebookGitBinding binding) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId firstChild =
          commitOnTopOf(
              repository, List.of(acceptedHead), "first.md", "first content", "First commit");
      ObjectId secondChild =
          commitOnTopOf(
              repository, List.of(firstChild), "second.md", "second content", "Second commit");
      return bundleBytesForHead(repository, secondChild);
    }
  }
}
