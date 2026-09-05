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
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BundleWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared notebook/bundle-crafting fixtures for the notebook Git-bundle download and proposal
 * controller tests ({@link NotebookGitBundleControllerTest}, {@link
 * NotebookGitProposalImportControllerTest}, {@link NotebookGitProposalAncestryControllerTest},
 * {@link NotebookGitProposalTreeShapeControllerTest}, {@link
 * NotebookGitProposalMarkdownFormatControllerTest}, and {@link
 * NotebookGitProposalPropertyValidationControllerTest}, and {@link
 * NotebookGitProjectionDriftControllerTest}).
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class NotebookGitBundleControllerTestBase extends NotebookControllerTestBase {

  private static final String FIXTURE_PREFIX = "notebook-git-proposal-committed-";

  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

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
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle("Git Backed Notebook For Bundle");
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  ResponseStatusException assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, HttpStatus expectedStatus)
      throws UnexpectedNoAccessRightException {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.publishNotebookGitProposal(notebook.getId(), expectedHead, bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(expectedStatus));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
    return exception;
  }

  static ObjectId commitOnTopOf(
      Repository repository, List<ObjectId> parents, String path, String content, String message)
      throws IOException {
    return commitOnTopOf(
        repository, parents, List.of(new NotebookGitProposalFile(path, content)), message);
  }

  static ObjectId commitOnTopOf(
      Repository repository,
      List<ObjectId> parents,
      List<NotebookGitProposalFile> files,
      String message)
      throws IOException {
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      DirCache dirCache = DirCache.newInCore();
      DirCacheBuilder builder = dirCache.builder();
      List<NotebookGitProposalFile> sortedByPath =
          files.stream().sorted(Comparator.comparing(NotebookGitProposalFile::path)).toList();
      for (NotebookGitProposalFile file : sortedByPath) {
        ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, file.contentBytes());
        DirCacheEntry entry = new DirCacheEntry(file.path());
        entry.setFileMode(file.mode());
        entry.setObjectId(blobId);
        builder.add(entry);
      }
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

  static byte[] bundleBytesForHead(Repository repository, ObjectId headId) throws IOException {
    RefUpdate refUpdate = repository.updateRef(Constants.R_HEADS + "main");
    refUpdate.setNewObjectId(headId);
    refUpdate.forceUpdate();

    BundleWriter bundleWriter = new BundleWriter(repository);
    bundleWriter.include(Constants.R_HEADS + "main", headId);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);
    return out.toByteArray();
  }

  /**
   * Testability-only: overwrites {@code notebook}'s accepted Git binding with a fresh root commit
   * built directly from {@code entries}, so proposal-gating tests can control the accepted tree's
   * exact shape without depending on the notebook's own note/folder content.
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
    return notebookGitBindingRepository.save(binding);
  }

  NotebookGitBinding snapshotCurrentPortableTree(Notebook notebook) {
    return notebookGitCutoverService.resnapshotForTestability(notebook, Instant.now());
  }

  /** A bundle whose {@code main} is a single-parent child of {@code binding}'s accepted head. */
  byte[] proposalBundleBytes(
      NotebookGitBinding binding, List<NotebookGitProposalFile> proposedFiles) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId childCommit =
          commitOnTopOf(repository, List.of(acceptedHead), proposedFiles, "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }

  /**
   * A frontmatter-valid baseline tree: one typed note, one typed README - clears tree-shape and
   * reaches the typed-Markdown gate, for tests whose proposal must get past both.
   */
  static List<PortableTreeEntry> validBaselineEntries() {
    return List.of(
        new PortableTreeEntry("note.md", "---\ntype: Note\n---\noriginal content"),
        new PortableTreeEntry("README.md", "---\ntype: Readme\n---\nreadme original"));
  }

  private <T> T committed(java.util.function.Supplier<T> action) {
    return inCommittedTransaction(transactionManager, action);
  }

  private void committed(Runnable action) {
    inCommittedTransaction(transactionManager, action);
  }
}
