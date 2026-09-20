package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter.BundleWriteResult;
import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

/**
 * Owns the accepted Git repository's persistence mechanics on behalf of every caller that opens or
 * stores a {@link NotebookGitBinding}'s accepted history: ordinary web changes, proposal
 * publication, creation/cutover/reset and bundle download. Objects and the accepted head live in
 * the native, JDBC-backed object store ({@code com.odde.donut.services.notebookGit.objectstore}),
 * on the connection bound to the surrounding Spring transaction, so a native write commits or rolls
 * back exactly with the rest of the business transaction. A binding whose native object store is
 * still empty (never touched by native storage, whatever its stored bundle bytes hold) is converted
 * from those bundle bytes the first time it is opened, so every caller ends up on native storage
 * through this one place - there is no endpoint-specific native mode or dual write.
 */
@Service
class NotebookGitAcceptedRepositoryStore {

  private final EntityPersister entityPersister;
  private final DataSource dataSource;

  NotebookGitAcceptedRepositoryStore(EntityPersister entityPersister, DataSource dataSource) {
    this.entityPersister = entityPersister;
    this.dataSource = dataSource;
  }

  /**
   * An accepted repository open for the caller's use, backed by the native object store and a JDBC
   * connection participating in the current Spring transaction. {@link #close} releases both; the
   * connection must never be closed directly.
   */
  record OpenedAcceptedRepository(
      Repository repository, ObjectId head, DataSource dataSource, Connection connection)
      implements AutoCloseable {
    @Override
    public void close() {
      repository.close();
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  /**
   * Opens {@code binding}'s accepted repository from the native object store, on the JDBC
   * connection bound to the current Spring transaction (so a caller's later native writes
   * commit/roll back with the rest of that transaction). If the native store has never been touched
   * for this binding - an untouched legacy binding, whatever its stored bundle bytes hold - its
   * bundle bytes are imported into the native store once before returning: every object the
   * bundle's {@code main} head reaches becomes a durable native row. The binding's accepted head
   * itself is untouched by this conversion; it was already correct. Creation/cutover/reset ({@link
   * #apply}) always populate the native store directly, so this path is only ever taken for a
   * binding that predates native storage.
   */
  OpenedAcceptedRepository open(NotebookGitBinding binding) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    Repository repository = new JdbcNotebookGitRepository(binding.getId(), connection);
    ObjectId head = ObjectId.fromString(binding.getAcceptedGitObjectId());
    if (repository.getObjectDatabase().getApproximateObjectCount() == 0) {
      importBundleIntoNativeStoreOnce(binding, repository, head);
    }
    return new OpenedAcceptedRepository(repository, head, dataSource, connection);
  }

  private void importBundleIntoNativeStoreOnce(
      NotebookGitBinding binding, Repository repository, ObjectId expectedHead) {
    try (NotebookGitBundleImporter.ImportedBundle legacy =
        NotebookGitBundleImporter.importAndVerifyMainHead(
            binding.getBundleBytes(), "accepted-bundle", expectedHead)) {
      copyAllReachableObjectsInto(repository, legacy.repository(), legacy.mainHead());
    }
  }

  /**
   * Writes {@code repository}'s {@code main} head as {@code binding}'s new accepted head and
   * persists the binding. When {@code repository} is already the binding's native store (an
   * ordinary web save appends directly against the repository {@link #open} returned), its objects
   * and head are already durable - written by the append itself - and only the JPA-managed entity
   * is brought back in sync. Otherwise (for example a proposal's imported repository), every object
   * {@code main} reaches is copied into the native store first.
   */
  String store(NotebookGitBinding binding, Repository repository, Timestamp updatedAt) {
    ObjectId newHead = mainHeadOf(repository);
    if (!(repository instanceof JdbcNotebookGitRepository)) {
      copyIntoNativeStore(binding, repository, newHead);
    }
    binding.setAcceptedGitObjectId(newHead.name());
    binding.setUpdatedAt(updatedAt);
    entityPersister.save(binding);
    return newHead.name();
  }

  /**
   * Copies every object {@code head} reaches from {@code source} into {@code binding}'s native
   * object store, on a connection scoped to this call and released immediately after. {@code
   * binding} must already have an ID: native object rows carry a foreign key to the binding row.
   */
  private void copyIntoNativeStore(NotebookGitBinding binding, Repository source, ObjectId head) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      Repository nativeRepository = new JdbcNotebookGitRepository(binding.getId(), connection);
      copyAllReachableObjectsInto(nativeRepository, source, head);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static void copyAllReachableObjectsInto(
      Repository target, Repository source, ObjectId head) {
    try (ObjectInserter inserter = target.newObjectInserter()) {
      NotebookGitReachableObjectCopier.copyAllReachableObjects(source, head, inserter);
      inserter.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static ObjectId mainHeadOf(Repository repository) {
    try {
      return repository.exactRef(Constants.R_HEADS + "main").getObjectId();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Writes {@code repository}'s {@code main} head as {@code binding}'s new accepted head and copies
   * every object it reaches into the native object store, for callers that own their own
   * persistence call (creation/cutover/reset, which additionally save through {@code
   * NotebookGitBindingRepository} alongside other binding fields such as {@code createdAt}). Native
   * object rows carry a foreign key to the binding row, so this persists {@code binding} first if
   * it does not have an ID yet (a brand-new binding); the caller's own later save then updates that
   * same already-persisted row rather than inserting a second one. {@code bundleBytes} is also set,
   * since the column remains {@code NOT NULL} - not to keep it live going forward, but because a
   * newly-written binding needs a value there regardless, exactly as before this slice.
   */
  String apply(NotebookGitBinding binding, Repository repository, Timestamp updatedAt) {
    BundleWriteResult written = NotebookGitBundleWriter.write(repository);
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(updatedAt);
    entityPersister.save(binding);
    copyIntoNativeStore(binding, repository, ObjectId.fromString(written.headObjectId()));
    return written.headObjectId();
  }

  /**
   * Opens {@code binding}'s accepted repository and re-serializes its {@code main} head into a
   * fresh, complete, cloneable bundle for transport download. The download's transport contract is
   * a reachable, cloneable bundle, not byte-for-byte equality with the stored representation.
   */
  byte[] downloadableBundle(NotebookGitBinding binding) {
    try (OpenedAcceptedRepository accepted = open(binding)) {
      return NotebookGitBundleWriter.write(accepted.repository()).bundleBytes();
    }
  }
}
