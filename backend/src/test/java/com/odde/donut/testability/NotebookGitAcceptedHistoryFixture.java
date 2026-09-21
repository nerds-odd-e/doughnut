package com.odde.donut.testability;

import com.odde.donut.services.notebookGit.NotebookGitReachableObjectCopier;
import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import javax.sql.DataSource;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Testability-only storage mechanics for putting a chosen Git history in place as a notebook Git
 * binding's accepted history: reading a fixture repository's {@code main} head, and writing every
 * object that head reaches into the binding's native accepted object store - the same JDBC-backed
 * store the product's own accepted repository reads, on the connection bound to the caller's
 * transaction.
 */
public final class NotebookGitAcceptedHistoryFixture {

  private NotebookGitAcceptedHistoryFixture() {}

  public static void seedNativeObjectStore(
      DataSource dataSource, Integer bindingId, Repository source, ObjectId head) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (Repository nativeRepository = new JdbcNotebookGitRepository(bindingId, connection);
        ObjectInserter inserter = nativeRepository.newObjectInserter()) {
      NotebookGitReachableObjectCopier.copyAllReachableObjects(source, head, inserter);
      inserter.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public static ObjectId mainHeadOf(Repository repository) {
    try {
      return repository.exactRef(Constants.R_HEADS + "main").getObjectId();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
