package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;

import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Shared notebook/binding JPA fixtures for notebook Git controller tests. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class NotebookGitControllerTestBase extends NotebookGitAcceptedObjectStoreTestSupport {

  private static final String FIXTURE_PREFIX = "notebook-git-proposal-committed-";

  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;

  private String testFixturePrefix;

  @BeforeEach
  void replaceDefaultFixtureWithCommittedUser() {
    String defaultUserExternalIdentifier = currentUser.getUser().getExternalIdentifier();
    committed(
        () -> deleteByUserExternalIdentifierLike(entityManager, defaultUserExternalIdentifier));
    testFixturePrefix = FIXTURE_PREFIX + UUID.randomUUID() + "-";
    committed(() -> currentUser.setUser(makeMe.aUser(testFixturePrefix + "owner").please()));
  }

  @AfterEach
  void cleanupCommittedFixture() {
    committed(
        () -> {
          String fixtureUsers = testFixturePrefix + "%";
          entityManager
              .createNativeQuery(
                  "DELETE s FROM subscription s "
                      + "INNER JOIN user subscriber ON s.user_id = subscriber.id "
                      + "INNER JOIN notebook nb ON s.notebook_id = nb.id "
                      + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                      + "INNER JOIN user owner ON o.user_id = owner.id "
                      + "WHERE subscriber.external_identifier LIKE :like "
                      + "OR owner.external_identifier LIKE :like")
              .setParameter("like", fixtureUsers)
              .executeUpdate();
          deleteByUserExternalIdentifierLike(entityManager, fixtureUsers);
        });
  }

  User createFixtureUser() {
    return makeMe.aUser(testFixturePrefix + "additional-user").please();
  }

  /** Real product creation, including the initial {@code .gitattributes}. */
  Notebook createGitBackedNotebook() throws UnexpectedNoAccessRightException {
    return createGitBackedNotebook("Git Backed Notebook");
  }

  Notebook createGitBackedNotebook(String title) throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle(title);
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  /** Stores {@code payload} in the notebook's content store and returns its LFS pointer. */
  byte[] pointerFor(Notebook notebook, byte[] payload) throws IOException {
    return notebookAttachmentContent.storeAsLfsPointer(notebook.getId(), payload);
  }

  /**
   * {@code tree} as the product commits it: each attachment's payload stored in the content store
   * and replaced by its pointer; notes, empty-folder markers, and Git metadata stay as they are.
   */
  List<PortableTreeEntry> asCommitted(Notebook notebook, List<PortableTreeEntry> tree)
      throws IOException {
    List<PortableTreeEntry> committed = new ArrayList<>();
    for (PortableTreeEntry entry : tree) {
      committed.add(
          isAttachment(entry)
              ? new PortableTreeEntry(entry.path(), pointerFor(notebook, entry.content()))
              : entry);
    }
    return committed;
  }

  /** Neither Markdown, an empty-folder marker, nor reserved Git metadata, at whatever depth. */
  static boolean isAttachment(PortableTreeEntry entry) {
    String path = entry.path();
    return !path.endsWith(".md")
        && !path.endsWith("/.keep")
        && !NotebookGitAttributes.isMetadataPath(path);
  }

  ResponseStatusException assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, HttpStatus expectedStatus)
      throws Exception {
    return NotebookGitProposalRejectionAssertions.assertRejectedWithoutMutatingBinding(
        controller,
        notebookGitBindingRepository,
        this::acceptedHistory,
        notebook,
        expectedHead,
        bundleBytes,
        expectedStatus);
  }

  <T extends RuntimeException> T assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, Class<T> exceptionType)
      throws Exception {
    return NotebookGitProposalRejectionAssertions.assertRejectedWithoutMutatingBinding(
        controller,
        notebookGitBindingRepository,
        this::acceptedHistory,
        notebook,
        expectedHead,
        bundleBytes,
        exceptionType);
  }

  NotebookGitBinding snapshotCurrentPortableTree(Notebook notebook) {
    return notebookGitCutoverService.resetHistory(notebook, Instant.now());
  }

  NotebookGitBinding reloadCommittedBinding(Integer notebookId) {
    return committed(
        () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
  }

  long countFoldersForNotebook(Integer notebookId) {
    return ((Number)
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM folder WHERE notebook_id = :notebookId")
                .setParameter("notebookId", notebookId)
                .getSingleResult())
        .longValue();
  }

  /** The notebook's current accepted history, served by its own download endpoint. */
  byte[] acceptedBundleBytes(Notebook notebook) throws Exception {
    return controller
        .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
        .getBody();
  }

  /** The accepted history the notebook's own download boundary currently serves. */
  AcceptedHistory acceptedHistory(Notebook notebook) throws Exception {
    return GitBundleTestReader.fetchAcceptedHistory(acceptedBundleBytes(notebook));
  }

  /** Single-parent proposal bundle on {@code binding}'s accepted head. */
  byte[] proposalBundleBytes(
      NotebookGitBinding binding, List<NotebookGitProposalFile> proposedFiles) throws Exception {
    Notebook notebook = notebookRepository.findById(binding.getNotebook().getId()).orElseThrow();
    return proposalBundleBytes(acceptedBundleBytes(notebook), proposedFiles);
  }

  void authorReferencingContentCommitted(Note note, String content) {
    committed(
        () ->
            authorReferencingContent(noteRepository.findById(note.getId()).orElseThrow(), content));
  }

  private <T> T committed(java.util.function.Supplier<T> action) {
    return inCommittedTransaction(transactionManager, action);
  }

  private void committed(Runnable action) {
    inCommittedTransaction(transactionManager, action);
  }
}
