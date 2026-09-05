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
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
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
  void ownerSubmittingAProposalStillReceivesTheInterimRefusal() throws Exception {
    Notebook notebook = createGitBackedNotebook();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, "someExpectedHead", validProposalBundleBytes()));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
  }

  @Test
  void rejectsUnreadableBundleBytesWithoutMutatingTheAcceptedBinding() throws Exception {
    assertProposalRejectedWithoutMutatingBinding(
        createGitBackedNotebook(), "not a git bundle".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void rejectsBundleWithoutUsableMainWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    assertProposalRejectedWithoutMutatingBinding(notebook, bundleBytesWithoutUsableMain());
  }

  private void assertProposalRejectedWithoutMutatingBinding(Notebook notebook, byte[] bundleBytes)
      throws UnexpectedNoAccessRightException {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.publishNotebookGitProposal(notebook, "someExpectedHead", bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
  }

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
