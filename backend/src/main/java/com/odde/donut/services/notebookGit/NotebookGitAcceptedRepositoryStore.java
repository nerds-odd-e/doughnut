package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
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
 * back exactly with the rest of the business transaction. Every caller reaches native storage
 * through this one place - there is no endpoint-specific storage mode or dual write.
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
   * commit/roll back with the rest of that transaction). Creation/cutover/reset ({@link #apply})
   * write every object the accepted head reaches, so a later read of an object missing from the
   * native store fails loudly rather than being repaired here.
   */
  OpenedAcceptedRepository open(NotebookGitBinding binding) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    Repository repository = new JdbcNotebookGitRepository(binding.getId(), connection);
    ObjectId head = ObjectId.fromString(binding.getAcceptedGitObjectId());
    return new OpenedAcceptedRepository(repository, head, dataSource, connection);
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
    try (ObjectInserter inserter =
        new JdbcNotebookGitRepository(binding.getId(), connection).newObjectInserter()) {
      NotebookGitReachableObjectCopier.copyAllReachableObjects(source, head, inserter);
      inserter.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
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
   * same already-persisted row rather than inserting a second one.
   */
  String apply(NotebookGitBinding binding, Repository repository, Timestamp updatedAt) {
    ObjectId head = mainHeadOf(repository);
    binding.setAcceptedGitObjectId(head.name());
    binding.setUpdatedAt(updatedAt);
    entityPersister.save(binding);
    copyIntoNativeStore(binding, repository, head);
    return head.name();
  }

  /**
   * Opens {@code binding}'s accepted repository and re-serializes its {@code main} head into a
   * fresh, complete, cloneable bundle for transport download. The download's transport contract is
   * a reachable, cloneable bundle, not byte-for-byte equality with any earlier serialization.
   */
  byte[] downloadableBundle(NotebookGitBinding binding) {
    try (OpenedAcceptedRepository accepted = open(binding)) {
      return NotebookGitBundleWriter.write(accepted.repository());
    }
  }
}
