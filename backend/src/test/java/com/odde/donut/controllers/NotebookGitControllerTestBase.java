package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookGit.NotebookGitTreeContent;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import com.odde.donut.testability.NotebookGitAcceptedHistoryFixture;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
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

  Notebook createGitBackedNotebook() throws UnexpectedNoAccessRightException {
    return createGitBackedNotebook("Git Backed Notebook");
  }

  /**
   * Product creation demoted to legacy RAW (most Git fixtures). Prefer {@link
   * #createProductLfsNotebook}.
   */
  Notebook createGitBackedNotebook(String title) throws UnexpectedNoAccessRightException {
    return demoteToLegacyRawBinding(createProductLfsNotebook(title));
  }

  /** Real product creation: LFS representation and initial {@code .gitattributes}. */
  Notebook createProductLfsNotebook() throws UnexpectedNoAccessRightException {
    return createProductLfsNotebook("Git Backed Notebook");
  }

  Notebook createProductLfsNotebook(String title) throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle(title);
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  /** Stores {@code payload} in the notebook's content store and returns its LFS pointer. */
  byte[] pointerFor(Notebook notebook, byte[] payload) throws IOException {
    String oid = sha256Hex(payload);
    assertThat(
        notebookAttachmentContent.store(
            notebook.getId(), oid, payload.length, new ByteArrayInputStream(payload)),
        is(true));
    return NotebookGitLfsPointer.format(oid, payload.length);
  }

  /**
   * {@code tree} as an LFS notebook commits it: each attachment's payload stored in the content
   * store and replaced by its pointer; notes, empty-folder markers, and Git metadata stay as they
   * are.
   */
  List<PortableTreeEntry> committedOnLfs(Notebook notebook, List<PortableTreeEntry> tree)
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

  Notebook demoteToLegacyRawBinding(Notebook notebook) {
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.RAW);
    notebookGitBindingRepository.save(binding);
    notebookGitCutoverService.resetHistory(notebook, Instant.now(), List.of());
    return notebook;
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

  /** Seeds accepted history from exact tree entries (disjoint from notebook content). */
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

  /** Seeds accepted history from an arbitrary repository tip into the native object store. */
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
