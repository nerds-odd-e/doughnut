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
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitTreeContent;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import com.odde.donut.testability.NotebookGitAcceptedHistoryFixture;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
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
abstract class NotebookGitControllerTestBase extends NotebookGitCommitFixtureTestSupport {

  private static final String FIXTURE_PREFIX = "notebook-git-proposal-committed-";

  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired DataSource dataSource;

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
    return createGitBackedNotebook("Git Backed Notebook");
  }

  Notebook createGitBackedNotebook(String title) throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle(title);
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  ResponseStatusException assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, HttpStatus expectedStatus)
      throws Exception {
    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, expectedHead, bundleBytes, ResponseStatusException.class);

    assertThat(exception.getStatusCode(), equalTo(expectedStatus));
    return exception;
  }

  <T extends RuntimeException> T assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, Class<T> exceptionType)
      throws Exception {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    String acceptedHeadBefore = before.getAcceptedGitObjectId();
    AcceptedHistory acceptedHistoryBefore = acceptedHistory(notebook);
    Instant updatedAtBefore = before.getUpdatedAt().toInstant();

    T exception =
        assertThrows(
            exceptionType,
            () ->
                controller.publishNotebookGitProposal(notebook.getId(), expectedHead, bundleBytes));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHeadBefore));
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
    assertThat(after.getUpdatedAt().toInstant(), equalTo(updatedAtBefore));
    return exception;
  }

  /**
   * Testability-only: replaces {@code notebook}'s accepted history with a fresh root commit built
   * directly from {@code entries}, so proposal-gating tests can control the accepted tree's exact
   * shape without depending on the notebook's own note/folder content.
   */
  NotebookGitBinding seedAcceptedBinding(Notebook notebook, List<PortableTreeEntry> entries) {
    try (Repository seeded =
        NotebookGitCommitBuilder.build(
            NotebookGitTreeContent.of(entries),
            "System",
            "system@example.com",
            "Seed content",
            Instant.now())) {
      return seedAcceptedHistory(
          notebook, seeded, NotebookGitAcceptedHistoryFixture.mainHeadOf(seeded));
    }
  }

  /**
   * Testability-only: makes {@code head} - and every object it reaches in {@code source} - {@code
   * notebook}'s accepted history, written into the same native object store the product's own
   * accepted repository reads. Deliberately disjoint from the notebook's own content, for drift,
   * invalid-tip and admission-rejection fixtures that must start from an accepted state the
   * product's creation/publication/reset owners would never produce.
   */
  NotebookGitBinding seedAcceptedHistory(Notebook notebook, Repository source, ObjectId head) {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    binding.setAcceptedGitObjectId(head.name());
    NotebookGitBinding saved = notebookGitBindingRepository.save(binding);
    committed(
        () ->
            NotebookGitAcceptedHistoryFixture.seedNativeObjectStore(
                dataSource, saved.getId(), source, head));
    return saved;
  }

  /**
   * Testability-only: deletes one object from {@code bindingId}'s native object store, leaving an
   * accepted history that reaches an object the store no longer holds.
   */
  void deleteNativeObjectStoreRow(Integer bindingId, String gitObjectId) {
    committed(
        () ->
            entityManager
                .createNativeQuery(
                    "DELETE FROM notebook_git_accepted_object WHERE notebook_git_binding_id ="
                        + " :bindingId AND git_object_id = :gitObjectId")
                .setParameter("bindingId", bindingId)
                .setParameter("gitObjectId", gitObjectId)
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

  /**
   * A bundle whose {@code main} is a single-parent child of {@code binding}'s accepted head, built
   * on the accepted history the download boundary currently serves.
   */
  byte[] proposalBundleBytes(
      NotebookGitBinding binding, List<NotebookGitProposalFile> proposedFiles) throws Exception {
    Notebook notebook = notebookRepository.findById(binding.getNotebook().getId()).orElseThrow();
    byte[] currentBundle = acceptedBundleBytes(notebook);
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
