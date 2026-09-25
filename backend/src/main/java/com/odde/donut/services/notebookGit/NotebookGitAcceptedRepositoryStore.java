package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.function.Predicate;
import javax.sql.DataSource;
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
   * commit/roll back with the rest of that transaction). Creation/cutover/reset ({@link #store})
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
   * Persists the caller's accepted head and binding timestamp. Imported repositories copy their
   * reachable objects first; native appends already wrote them in the current transaction. A new
   * binding must be inserted before copying because native objects reference its row.
   */
  String store(
      NotebookGitBinding binding, Repository repository, ObjectId head, Timestamp updatedAt) {
    binding.setAcceptedGitObjectId(head.name());
    binding.setUpdatedAt(updatedAt);
    if (binding.getId() == null) {
      entityPersister.save(binding);
    }
    if (!(repository instanceof JdbcNotebookGitRepository)) {
      copyIntoNativeStore(binding, repository, head);
    }
    entityPersister.save(binding);
    return head.name();
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

  /**
   * Whether {@code binding}'s accepted tree has a file, note or folder at a path. The tree is read
   * once, so the result can check many candidate paths.
   */
  Predicate<String> takenPaths(NotebookGitBinding binding) {
    try (OpenedAcceptedRepository accepted = open(binding)) {
      return NotebookGitAcceptedTree.takenPaths(accepted.repository(), accepted.head());
    }
  }
}
