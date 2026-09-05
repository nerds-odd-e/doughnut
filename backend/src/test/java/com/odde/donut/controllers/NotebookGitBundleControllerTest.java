package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BundleWriter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitBundleControllerTest extends NotebookControllerTestBase {

  private Notebook createGitBackedNotebook() throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle("Git Backed Notebook For Bundle");
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  @Test
  void ownerDownloadsAcceptedBundleWithoutMutatingIt() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseEntity<byte[]> response = controller.downloadNotebookGitBundle(notebook);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(
        response.getHeaders().getContentType(),
        equalTo(MediaType.valueOf("application/x-git-bundle")));
    assertThat(
        response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
        containsString("attachment;"));
    assertThat(response.getBody(), equalTo(before.getBundleBytes()));

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, response.getBody());
      assertThat(headObjectId.getName(), equalTo(before.getAcceptedGitObjectId()));
    }

    // A system `git clone` of this bundle must check out "main" regardless of the cloning
    // machine's own `init.defaultBranch`, which only happens if the bundle advertises HEAD.
    ObjectId advertisedHead = GitBundleTestReader.fetchAdvertisedHead(response.getBody());
    assertThat(advertisedHead, notNullValue());
    assertThat(advertisedHead.getName(), equalTo(before.getAcceptedGitObjectId()));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getId(), equalTo(before.getId()));
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
  }

  @Test
  void deniesDownloadForNotebookOwnedByAnotherUser() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadNotebookGitBundle(notebook));
  }

  @Test
  void deniesDownloadForReadOnlySubscriber() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    User subscriber = makeMe.aUser().please();
    makeMe.aSubscription().forNotebook(notebook).forUser(subscriber).please();
    currentUser.setUser(subscriber);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadNotebookGitBundle(notebook));
  }

  @Test
  void ownerSubmittingAValidChildProposalStillReceivesTheInterimRefusal() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook,
                    binding.getAcceptedGitObjectId(),
                    singleParentChildBundleBytes(binding)));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

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
                    notebook, binding.getAcceptedGitObjectId(), identicalHeadBundleBytes(binding)));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void rejectsUnreadableBundleBytesWithoutMutatingTheAcceptedBinding() throws Exception {
    assertProposalRejectedWithoutMutatingBinding(
        createGitBackedNotebook(),
        "someExpectedHead",
        "not a git bundle".getBytes(StandardCharsets.UTF_8),
        HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsBundleWithoutUsableMainWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    assertProposalRejectedWithoutMutatingBinding(
        notebook, "someExpectedHead", bundleBytesWithoutUsableMain(), HttpStatus.BAD_REQUEST);
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

  private void assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, HttpStatus expectedStatus)
      throws UnexpectedNoAccessRightException {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.publishNotebookGitProposal(notebook, expectedHead, bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(expectedStatus));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
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

  /** A well-formed bundle that advertises {@code refs/heads/other}, not {@code refs/heads/main}. */
  private byte[] bundleBytesWithoutUsableMain() throws IOException {
    List<PortableTreeEntry> entries =
        List.of(new PortableTreeEntry("README.md", "off-main content"));
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "Proposer", "proposer@example.com", "Off-main commit", Instant.now())) {
      ObjectId commitId = repository.exactRef(Constants.R_HEADS + "main").getObjectId();

      BundleWriter bundleWriter = new BundleWriter(repository);
      bundleWriter.include("refs/heads/other", commitId);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);
      return out.toByteArray();
    }
  }

  /** A bundle whose {@code main} is the accepted head itself - no new commits. */
  private byte[] identicalHeadBundleBytes(NotebookGitBinding binding) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      return bundleBytesForHead(repository, acceptedHead);
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

  private static ObjectId commitOnTopOf(
      Repository repository, List<ObjectId> parents, String path, String content, String message)
      throws IOException {
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      ObjectId blobId =
          inserter.insert(Constants.OBJ_BLOB, content.getBytes(StandardCharsets.UTF_8));
      DirCache dirCache = DirCache.newInCore();
      DirCacheBuilder builder = dirCache.builder();
      DirCacheEntry entry = new DirCacheEntry(path);
      entry.setFileMode(FileMode.REGULAR_FILE);
      entry.setObjectId(blobId);
      builder.add(entry);
      builder.finish();
      ObjectId treeId = dirCache.writeTree(inserter);

      PersonIdent author =
          new PersonIdent("Proposer", "proposer@example.com", Instant.now(), ZoneOffset.UTC);
      CommitBuilder commitBuilder = new CommitBuilder();
      commitBuilder.setTreeId(treeId);
      commitBuilder.setParentIds(parents.toArray(new ObjectId[0]));
      commitBuilder.setAuthor(author);
      commitBuilder.setCommitter(author);
      commitBuilder.setMessage(message);
      ObjectId commitId = inserter.insert(commitBuilder);
      inserter.flush();
      return commitId;
    }
  }

  private static byte[] bundleBytesForHead(Repository repository, ObjectId headId)
      throws IOException {
    RefUpdate refUpdate = repository.updateRef(Constants.R_HEADS + "main");
    refUpdate.setNewObjectId(headId);
    refUpdate.forceUpdate();

    BundleWriter bundleWriter = new BundleWriter(repository);
    bundleWriter.include(Constants.R_HEADS + "main", headId);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);
    return out.toByteArray();
  }

  @Test
  void deniesPublishForNotebookOwnedByAnotherUser() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            controller.publishNotebookGitProposal(
                notebook, "someExpectedHead", "placeholder bundle bytes".getBytes()));
  }

  @Test
  void deniesPublishForReadOnlySubscriber() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    User subscriber = makeMe.aUser().please();
    makeMe.aSubscription().forNotebook(notebook).forUser(subscriber).please();
    currentUser.setUser(subscriber);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            controller.publishNotebookGitProposal(
                notebook, "someExpectedHead", "placeholder bundle bytes".getBytes()));
  }
}
