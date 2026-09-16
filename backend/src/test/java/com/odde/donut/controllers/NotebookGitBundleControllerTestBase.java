package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Note;
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

/** Shared notebook/bundle-crafting fixtures for notebook Git controller tests. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class NotebookGitBundleControllerTestBase extends NotebookControllerTestBase {

  private static final String FIXTURE_PREFIX = "notebook-git-proposal-committed-";

  /**
   * Substantial typed-note body used as the original baseline for JGit rename scoring. Multiple
   * non-trivial prose lines ensure shared frontmatter cannot dominate the similarity score.
   */
  static final String SUBSTANTIAL_ORIGINAL_BODY =
      "---\ntype: Note\n---\n"
          + "The quick brown fox jumps over the lazy dog near the riverbank.\n"
          + "She decided to read the ancient manuscript that described the valley.\n"
          + "Mountains rose in the distance, their peaks covered with fresh snow.\n"
          + "A small village nestled between them kept its traditions alive for generations.\n"
          + "Travelers came each spring to trade cloth and spices at the market.\n"
          + "Children played near the fountain while elders discussed the harvest.\n"
          + "The librarian organized every scroll by region and by season.\n";

  /**
   * Substantial typed-note body topically unrelated to {@link #SUBSTANTIAL_ORIGINAL_BODY}; their
   * JGit similarity is well below the configured 50% threshold, so a removal/addition pair using
   * these two bodies stays an unresolved mixture rather than a detected rename.
   */
  static final String SUBSTANTIAL_UNRELATED_BODY =
      "---\ntype: Note\n---\n"
          + "Quantum entanglement links particles across vast distances instantly.\n"
          + "Researchers measured photon spins in supercooled vacuum chambers.\n"
          + "The experiment required precision instruments and calm steady hands.\n"
          + "Equations described probability amplitudes rather than certainties.\n"
          + "Funding agencies reviewed the proposal for six months before approving.\n"
          + "Graduate students calibrated lasers late into the quiet night.\n"
          + "A paper summarizing findings was submitted to a prominent journal.\n";

  /**
   * Substantial typed-note body edited from {@link #SUBSTANTIAL_ORIGINAL_BODY} so the JGit
   * similarity score lands in [50, 60): distinguishes the configured 50% rename policy from JGit's
   * 60% default while genuinely changing content. Shared by inferred move-and-edit reference
   * fixtures across rename and relocation referrer controller tests.
   */
  static final String MODERATE_EDIT_BODY =
      "---\ntype: Note\n---\n"
          + "The quick brown fox jumps over the lazy dog near the riverbank.\n"
          + "She decided to read the ancient manuscript that described the valley.\n"
          + "Mountains rose in the distance, their peaks covered with fresh snow.\n"
          + "A small village nestled between them kept its traditions alive for generations.\n"
          + "Completely rewritten prose about oceans and sailing ships replaces this.\n"
          + "Sailors navigated by stars across the wide and stormy open waters.\n"
          + "The harbor master logged each vessel and collected the docking fees.\n";

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

  /** Counts rows in {@code table} that reference the given note via {@code note_id}. */
  protected long countRowsByNoteId(String table, Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  /** Counts recall prompts whose memory tracker belongs to the given note. */
  protected long countRecallPromptsByNoteId(Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM recall_prompt rp "
                        + "JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
                        + "WHERE mt.note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  /** Counts conversation_message rows belonging to any conversation of the given note. */
  protected long countConversationMessagesByNoteId(Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM conversation_message cm "
                        + "JOIN conversation c ON cm.conversation_id = c.id "
                        + "WHERE c.note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  /** Aggregates the complete note-dependent closure counts for absence/survival assertions. */
  protected DependentCounts dependentCounts(Note note) {
    return new DependentCounts(
        countRowsByNoteId("memory_tracker", note.getId()),
        countRecallPromptsByNoteId(note.getId()),
        countRowsByNoteId("mcq", note.getId()),
        countRowsByNoteId("image", note.getId()),
        countRowsByNoteId("conversation", note.getId()),
        countConversationMessagesByNoteId(note.getId()));
  }

  /** Snapshot of the complete note-dependent row counts used by deletion/retry proofs. */
  protected record DependentCounts(
      long memoryTracker,
      long recallPrompt,
      long mcq,
      long image,
      long conversation,
      long conversationMessage) {
    static DependentCounts allAbsent() {
      return new DependentCounts(0, 0, 0, 0, 0, 0);
    }
  }

  private <T> T committed(java.util.function.Supplier<T> action) {
    return inCommittedTransaction(transactionManager, action);
  }

  private void committed(Runnable action) {
    inCommittedTransaction(transactionManager, action);
  }
}
