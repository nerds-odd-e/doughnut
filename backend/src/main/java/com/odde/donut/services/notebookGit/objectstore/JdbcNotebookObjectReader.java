package com.odde.donut.services.notebookGit.objectstore;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;

/**
 * Reads objects straight from {@code notebook_git_accepted_object} through the shared connection -
 * no process-local cache, so a reader always sees exactly what its connection's current transaction
 * sees. When constructed from an {@link JdbcNotebookObjectInserter} session, it also checks that
 * inserter's still-unflushed buffer first, so recently inserted objects are readable before {@code
 * flush()} as JGit's {@code ObjectInserter#newReader()} contract requires.
 */
final class JdbcNotebookObjectReader extends ObjectReader {

  private final JdbcNotebookObjectDatabase database;
  private final Map<ObjectId, JdbcNotebookObjectInserter.BufferedObject> buffered;

  JdbcNotebookObjectReader(JdbcNotebookObjectDatabase database) {
    this(database, Map.of());
  }

  JdbcNotebookObjectReader(
      JdbcNotebookObjectDatabase database,
      Map<ObjectId, JdbcNotebookObjectInserter.BufferedObject> buffered) {
    this.database = database;
    this.buffered = buffered;
  }

  @Override
  public ObjectReader newReader() {
    return new JdbcNotebookObjectReader(database, buffered);
  }

  @Override
  public Collection<ObjectId> resolve(AbbreviatedObjectId id) throws IOException {
    Set<ObjectId> matches = new HashSet<>();
    for (ObjectId candidate : buffered.keySet()) {
      if (id.prefixCompare(candidate) == 0) {
        matches.add(candidate);
      }
    }
    matches.addAll(database.resolvePrefix(id));
    return matches;
  }

  @Override
  public ObjectLoader open(AnyObjectId objectId, int typeHint)
      throws MissingObjectException, IncorrectObjectTypeException, IOException {
    ObjectId id = objectId.copy();
    JdbcNotebookObjectInserter.BufferedObject inFlight = buffered.get(id);
    if (inFlight != null) {
      return checkType(id, inFlight.type(), inFlight.data(), typeHint);
    }
    Optional<JdbcNotebookObjectDatabase.StoredObject> stored = database.find(id);
    if (stored.isEmpty()) {
      throw typeHint == OBJ_ANY
          ? new MissingObjectException(id, "unknown")
          : new MissingObjectException(id, typeHint);
    }
    return checkType(id, stored.get().type(), stored.get().data(), typeHint);
  }

  private static ObjectLoader checkType(ObjectId id, int actualType, byte[] data, int typeHint)
      throws IncorrectObjectTypeException {
    if (typeHint != OBJ_ANY && typeHint != actualType) {
      throw new IncorrectObjectTypeException(id, typeHint);
    }
    return new ObjectLoader.SmallObject(actualType, data);
  }

  @Override
  public Set<ObjectId> getShallowCommits() {
    return Set.of();
  }

  @Override
  public void close() {
    // The connection is a caller-supplied dependency; this reader never closes it.
  }
}
