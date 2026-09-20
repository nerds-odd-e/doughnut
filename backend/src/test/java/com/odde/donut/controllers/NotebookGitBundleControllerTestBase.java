package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.testability.GitBundleTestReader;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Shared notebook/binding JPA fixtures for notebook Git controller tests. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class NotebookGitBundleControllerTestBase extends NotebookGitCommitFixtureTestSupport {

  private static final String FIXTURE_PREFIX = "notebook-git-proposal-committed-";

  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired PlatformTransactionManager transactionManager;

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

  Notebook createGitBackedNotebook() throws UnexpectedNoAccessRightException {
    return createGitBackedNotebook("Git Backed Notebook For Bundle");
  }

  Notebook createGitBackedNotebook(String title) throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle(title);
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  ResponseStatusException assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, HttpStatus expectedStatus)
      throws UnexpectedNoAccessRightException {
    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, expectedHead, bundleBytes, ResponseStatusException.class);

    assertThat(exception.getStatusCode(), equalTo(expectedStatus));
    return exception;
  }

  <T extends RuntimeException> T assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, Class<T> exceptionType)
      throws UnexpectedNoAccessRightException {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    String acceptedHeadBefore = before.getAcceptedGitObjectId();
    byte[] acceptedBundleBefore = before.getBundleBytes().clone();
    Instant updatedAtBefore = before.getUpdatedAt().toInstant();

    T exception =
        assertThrows(
            exceptionType,
            () ->
                controller.publishNotebookGitProposal(notebook.getId(), expectedHead, bundleBytes));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHeadBefore));
    assertThat(after.getBundleBytes(), equalTo(acceptedBundleBefore));
    assertThat(after.getUpdatedAt().toInstant(), equalTo(updatedAtBefore));
    return exception;
  }

  /**
   * Testability-only: overwrites {@code notebook}'s accepted Git binding with a fresh root commit
   * built directly from {@code entries}, so proposal-gating tests can control the accepted tree's
   * exact shape without depending on the notebook's own note/folder content. This sets the
   * binding's entity fields directly rather than going through {@code
   * NotebookGitAcceptedRepositoryStore}, so it also clears any native object-store rows the binding
   * already had: otherwise a binding already converted to native storage would keep a nonzero
   * object count and never re-trigger the lazy bundle-to-native conversion for this newly-seeded,
   * disjoint history.
   */
  NotebookGitBinding seedAcceptedBinding(Notebook notebook, List<PortableTreeEntry> entries) {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "System", "system@example.com", "Seed content", Instant.now())) {
      NotebookGitBundleWriter.BundleWriteResult written = NotebookGitBundleWriter.write(repository);
      binding.setAcceptedGitObjectId(written.headObjectId());
      binding.setBundleBytes(written.bundleBytes());
    }
    NotebookGitBinding saved = notebookGitBindingRepository.save(binding);
    clearNativeObjectStoreRows(saved.getId());
    return saved;
  }

  /**
   * Testability-only: deletes every native object-store row for {@code bindingId}. Any test that
   * sets {@code NotebookGitBinding#acceptedGitObjectId}/{@code bundleBytes} directly (bypassing
   * {@code NotebookGitAcceptedRepositoryStore}) to seed a specific, disjoint accepted tree must
   * call this afterward: otherwise a binding already converted to native storage keeps a nonzero
   * native object count and never re-triggers the lazy bundle-to-native conversion for the
   * newly-seeded head, so reads against the new head find its objects missing from the native
   * store.
   */
  void clearNativeObjectStoreRows(Integer bindingId) {
    committed(
        () ->
            entityManager
                .createNativeQuery(
                    "DELETE FROM notebook_git_accepted_object WHERE notebook_git_binding_id ="
                        + " :bindingId")
                .setParameter("bindingId", bindingId)
                .executeUpdate());
  }

  /** Testability-only: counts {@code bindingId}'s native object-store rows. */
  long countNativeObjectStoreRows(Integer bindingId) {
    return committed(
        () ->
            ((Number)
                    entityManager
                        .createNativeQuery(
                            "SELECT COUNT(*) FROM notebook_git_accepted_object "
                                + "WHERE notebook_git_binding_id = :bindingId")
                        .setParameter("bindingId", bindingId)
                        .getSingleResult())
                .longValue());
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

  /**
   * A bundle whose {@code main} is a single-parent child of {@code binding}'s accepted head. Reads
   * the current accepted head/history through the notebook's own download endpoint rather than
   * {@code binding.getBundleBytes()} directly: once a binding's ordinary saves move onto native
   * object storage, that column is no longer kept in sync with the accepted head, so only the
   * download's live, re-serialized bundle reliably reflects the current accepted history.
   */
  byte[] proposalBundleBytes(
      NotebookGitBinding binding, List<NotebookGitProposalFile> proposedFiles) throws Exception {
    Notebook notebook = notebookRepository.findById(binding.getNotebook().getId()).orElseThrow();
    byte[] currentBundle = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, currentBundle);
      ObjectId childCommit =
          commitOnTopOf(repository, List.of(acceptedHead), proposedFiles, "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }

  private <T> T committed(java.util.function.Supplier<T> action) {
    return inCommittedTransaction(transactionManager, action);
  }

  private void committed(Runnable action) {
    inCommittedTransaction(transactionManager, action);
  }
}
