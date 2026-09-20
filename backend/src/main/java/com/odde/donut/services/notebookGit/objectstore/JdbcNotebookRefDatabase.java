package com.odde.donut.services.notebookGit.objectstore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Reads and updates the notebook's one branch, {@code refs/heads/main}, directly off {@link
 * com.odde.donut.entities.NotebookGitBinding#getAcceptedGitObjectId()} - the product's existing
 * single source of truth for a binding's accepted head - rather than inventing a second ref
 * authority. {@code HEAD} is always a symbolic ref to {@code refs/heads/main}; no other ref name is
 * recognized, matching this product's single-branch model (see ADR 0002).
 *
 * <p>No process-local cache: every read queries the connection directly, so a freshly reopened
 * instance always reflects the connection's current transactional view.
 */
final class JdbcNotebookRefDatabase extends RefDatabase {

  private static final String MAIN = Constants.R_HEADS + "main";

  private final Repository repository;
  private final int notebookGitBindingId;
  private final Connection connection;

  JdbcNotebookRefDatabase(Repository repository, int notebookGitBindingId, Connection connection) {
    this.repository = repository;
    this.notebookGitBindingId = notebookGitBindingId;
    this.connection = connection;
  }

  @Override
  public void create() {
    // The owning NotebookGitBinding row is created by existing binding-creation code; there is
    // nothing further for this ref database to initialize.
  }

  @Override
  public void close() {
    // The connection is a caller-supplied dependency; this ref database never closes it.
  }

  @Override
  public boolean isNameConflicting(String refName) {
    // This store recognizes exactly "HEAD" and "refs/heads/main"; neither can ever be nested
    // under the other, so no name it recognizes conflicts with another.
    return false;
  }

  @Override
  public Ref exactRef(String name) throws IOException {
    if (Constants.HEAD.equals(name)) {
      Ref target = readMain();
      if (target == null) {
        target = new ObjectIdRef.Unpeeled(Ref.Storage.NEW, MAIN, null);
      }
      return new SymbolicRef(Constants.HEAD, target);
    }
    if (MAIN.equals(name)) {
      return readMain();
    }
    return null;
  }

  @Override
  public Map<String, Ref> getRefs(String prefix) throws IOException {
    Map<String, Ref> refs = new HashMap<>();
    putIfMatching(refs, prefix, exactRef(Constants.HEAD));
    putIfMatching(refs, prefix, exactRef(MAIN));
    return refs;
  }

  private static void putIfMatching(Map<String, Ref> refs, String prefix, Ref ref) {
    if (ref != null && ref.getName().startsWith(prefix)) {
      refs.put(ref.getName().substring(prefix.length()), ref);
    }
  }

  @Override
  public List<Ref> getAdditionalRefs() {
    return List.of();
  }

  @Override
  public Ref peel(Ref ref) throws IOException {
    Ref leaf = ref.getLeaf();
    if (leaf.isPeeled() || leaf.getObjectId() == null) {
      return ref;
    }
    Ref peeledLeaf = doPeel(leaf);
    return ref.isSymbolic() ? new SymbolicRef(ref.getName(), peeledLeaf) : peeledLeaf;
  }

  private Ref doPeel(Ref leaf) throws IOException {
    try (RevWalk walk = new RevWalk(repository)) {
      RevObject object = walk.parseAny(leaf.getObjectId());
      if (object instanceof RevTag) {
        return new ObjectIdRef.PeeledTag(
            leaf.getStorage(), leaf.getName(), leaf.getObjectId(), walk.peel(object).copy());
      }
      return new ObjectIdRef.PeeledNonTag(leaf.getStorage(), leaf.getName(), leaf.getObjectId());
    }
  }

  @Override
  public ReflogReader getReflogReader(Ref ref) {
    return EmptyReflogReader.INSTANCE;
  }

  @Override
  public RefUpdate newUpdate(String refName, boolean detach) throws IOException {
    Ref ref = exactRef(refName);
    boolean detachingSymbolicRef = false;
    if (ref == null) {
      ref = new ObjectIdRef.Unpeeled(Ref.Storage.NEW, refName, null);
    } else {
      detachingSymbolicRef = detach && ref.isSymbolic();
    }
    JdbcNotebookRefUpdate update = new JdbcNotebookRefUpdate(this, ref, repository);
    if (detachingSymbolicRef) {
      update.setDetachingSymbolicRef();
    }
    return update;
  }

  @Override
  public RefRename newRename(String fromName, String toName) {
    throw new UnsupportedOperationException(
        "This store has one fixed branch, refs/heads/main; ref rename is not supported.");
  }

  private Ref readMain() throws IOException {
    String sql = "SELECT accepted_git_object_id FROM notebook_git_binding WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookGitBindingId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        ObjectId id = ObjectId.fromString(resultSet.getString(1));
        return new ObjectIdRef.Unpeeled(Ref.Storage.PACKED, MAIN, id);
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  /**
   * Compare-and-put against the binding's {@code accepted_git_object_id} column, only for {@code
   * refs/heads/main} - the only ref this store persists. {@code expectedOld == null} can never
   * match, since the column is {@code NOT NULL}; that correctly yields a lock failure rather than a
   * wasted round trip.
   */
  boolean compareAndPutMain(String refName, ObjectId expectedOld, ObjectId newValue)
      throws IOException {
    if (!MAIN.equals(refName)) {
      throw new UnsupportedOperationException(
          "This store only persists refs/heads/main, not " + refName);
    }
    if (expectedOld == null) {
      return false;
    }
    String sql =
        "UPDATE notebook_git_binding SET accepted_git_object_id = ? "
            + "WHERE id = ? AND accepted_git_object_id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, newValue.name());
      statement.setInt(2, notebookGitBindingId);
      statement.setString(3, expectedOld.name());
      return statement.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  private static final class EmptyReflogReader implements ReflogReader {
    static final EmptyReflogReader INSTANCE = new EmptyReflogReader();

    @Override
    public ReflogEntry getLastEntry() {
      return null;
    }

    @Override
    public List<ReflogEntry> getReverseEntries() {
      return List.of();
    }

    @Override
    public ReflogEntry getReverseEntry(int number) {
      return null;
    }

    @Override
    public List<ReflogEntry> getReverseEntries(int max) {
      return List.of();
    }
  }
}
