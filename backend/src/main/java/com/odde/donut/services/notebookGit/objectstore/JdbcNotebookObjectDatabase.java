package com.odde.donut.services.notebookGit.objectstore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Owns the JDBC access to one binding's rows of {@code notebook_git_accepted_object}: one row per
 * Git object, content-addressed by its 40-hex object ID (naturally idempotent/deduplicated).
 *
 * <p>Reads ({@link #find}, {@link #resolvePrefix}) always query the live connection directly - no
 * process-local cache - so a freshly reopened instance always sees exactly what the connection's
 * current transaction sees. {@link #insertMissing} is the batched write path: one existence-check
 * query for a whole flushed {@link JdbcNotebookObjectInserter} session, then one batched insert for
 * whatever is actually missing, instead of a round trip per attempted object.
 */
final class JdbcNotebookObjectDatabase extends ObjectDatabase {

  /** Conservative bound on how many placeholders one {@code IN (...)}/multi-row INSERT carries. */
  private static final int MAX_BATCH_SIZE = 500;

  private final int notebookGitBindingId;
  private final Connection connection;

  JdbcNotebookObjectDatabase(int notebookGitBindingId, Connection connection) {
    this.notebookGitBindingId = notebookGitBindingId;
    this.connection = connection;
  }

  /** One Git object's type and raw bytes, whether stored, preloaded, or awaiting flush. */
  record ObjectContent(int type, byte[] data) {}

  @Override
  public JdbcNotebookObjectInserter newInserter() {
    return new JdbcNotebookObjectInserter(this);
  }

  @Override
  public JdbcNotebookObjectReader newReader() {
    return new JdbcNotebookObjectReader(this);
  }

  @Override
  public void close() {
    // The connection is a caller-supplied dependency; this database never closes it.
  }

  @Override
  public long getApproximateObjectCount() {
    String sql =
        "SELECT COUNT(*) FROM notebook_git_accepted_object WHERE notebook_git_binding_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookGitBindingId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
    } catch (SQLException e) {
      throw new UncheckedIOException(new IOException(e));
    }
  }

  /**
   * A reader served from one bulk read of every object this binding stores, for a caller that walks
   * the whole history (a bundle download). The loaded objects live only as long as that reader.
   */
  JdbcNotebookObjectReader newWholeHistoryReader() throws IOException {
    String sql =
        "SELECT object_type, object_bytes, git_object_id FROM notebook_git_accepted_object "
            + "WHERE notebook_git_binding_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookGitBindingId);
      try (ResultSet resultSet = statement.executeQuery()) {
        Map<ObjectId, ObjectContent> loaded = new HashMap<>();
        while (resultSet.next()) {
          loaded.put(
              ObjectId.fromString(resultSet.getString(3)),
              new ObjectContent(resultSet.getInt(1), resultSet.getBytes(2)));
        }
        return new JdbcNotebookObjectReader(this, loaded);
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  Optional<ObjectContent> find(ObjectId id) throws IOException {
    String sql =
        "SELECT object_type, object_bytes FROM notebook_git_accepted_object "
            + "WHERE notebook_git_binding_id = ? AND git_object_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookGitBindingId);
      statement.setString(2, id.name());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(new ObjectContent(resultSet.getInt(1), resultSet.getBytes(2)));
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  Set<ObjectId> resolvePrefix(AbbreviatedObjectId prefix) throws IOException {
    String sql =
        "SELECT git_object_id FROM notebook_git_accepted_object "
            + "WHERE notebook_git_binding_id = ? AND git_object_id LIKE ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookGitBindingId);
      statement.setString(2, prefix.name() + "%");
      try (ResultSet resultSet = statement.executeQuery()) {
        Set<ObjectId> matches = new HashSet<>();
        while (resultSet.next()) {
          matches.add(ObjectId.fromString(resultSet.getString(1)));
        }
        return matches;
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  /**
   * The batched flush path: one existence-check query per chunk of the whole buffered set (one
   * chunk for an ordinary save), then one batched insert per chunk of whatever is actually missing.
   * This is what keeps a multi-tree {@code DirCacheBuilder} append - which reattempts an insert for
   * every tree in the hierarchy, not just the changed path - from costing one SQL round trip per
   * attempted object.
   */
  void insertMissing(Map<ObjectId, ObjectContent> buffered) throws IOException {
    if (buffered.isEmpty()) {
      return;
    }
    List<ObjectId> pending = new ArrayList<>(buffered.keySet());
    Set<ObjectId> existing = new HashSet<>();
    for (List<ObjectId> chunk : chunks(pending, MAX_BATCH_SIZE)) {
      existing.addAll(existingIds(chunk));
    }
    List<ObjectId> missing = pending.stream().filter(id -> !existing.contains(id)).toList();
    for (List<ObjectId> chunk : chunks(missing, MAX_BATCH_SIZE)) {
      insertRows(chunk, buffered);
    }
  }

  private Set<ObjectId> existingIds(List<ObjectId> ids) throws IOException {
    String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    String sql =
        "SELECT git_object_id FROM notebook_git_accepted_object "
            + "WHERE notebook_git_binding_id = ? AND git_object_id IN ("
            + placeholders
            + ")";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookGitBindingId);
      int index = 2;
      for (ObjectId id : ids) {
        statement.setString(index++, id.name());
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        Set<ObjectId> found = new HashSet<>();
        while (resultSet.next()) {
          found.add(ObjectId.fromString(resultSet.getString(1)));
        }
        return found;
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  private void insertRows(List<ObjectId> ids, Map<ObjectId, ObjectContent> buffered)
      throws IOException {
    if (ids.isEmpty()) {
      return;
    }
    String row = "(?, ?, ?, ?)";
    String sql =
        "INSERT INTO notebook_git_accepted_object "
            + "(notebook_git_binding_id, git_object_id, object_type, object_bytes) VALUES "
            + String.join(",", Collections.nCopies(ids.size(), row));
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int index = 1;
      for (ObjectId id : ids) {
        ObjectContent object = buffered.get(id);
        statement.setInt(index++, notebookGitBindingId);
        statement.setString(index++, id.name());
        statement.setInt(index++, object.type());
        statement.setBytes(index++, object.data());
      }
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  private static List<List<ObjectId>> chunks(List<ObjectId> ids, int size) {
    List<List<ObjectId>> chunks = new ArrayList<>();
    for (int start = 0; start < ids.size(); start += size) {
      chunks.add(ids.subList(start, Math.min(start + size, ids.size())));
    }
    return chunks;
  }
}
